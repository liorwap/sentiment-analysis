package com.serverless;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.JSONArray;

public class ProcessStoriesByPhrase implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger LOG = LogManager.getLogger(ProcessStoriesByPhrase.class);
	private static final Analytics analyzer = new Analytics();
	private static final ReentrantLock lock = new ReentrantLock();

	private static void asyncAnalyzeAndTraverseStoryComments(JSONArray comments) throws UnirestException, InterruptedException, ExecutionException {
		if(comments == null || comments.length() == 0) return;
		List<String> commentsTexts = HackerNewsAPI.asyncGetCommentsTexts(comments);
		List<SentimentAnalysisAPI.Analysis> sentimentsAnalytics = SentimentAnalysisAPI.asyncGetSentimentsAnalysis(commentsTexts);
		for(SentimentAnalysisAPI.Analysis sentimentAnalytic: sentimentsAnalytics){
			lock.lock();
			analyzer.
					update(sentimentAnalytic.getAttribute(),
					sentimentAnalytic.getRatio(),
					sentimentAnalytic.getScore());
			lock.unlock();
		}
		//dfs
		List<JSONArray> lowerLevelComments = HackerNewsAPI.asyncGetItemsComments(comments);
		parsingCommentsTasksMaker(lowerLevelComments);
	}
	static class parseComments implements Callable<String> {
		JSONArray comments;
		public parseComments(JSONArray comments) {
			this.comments = comments;
		}

		@Override
		public String call() throws InterruptedException, UnirestException, ExecutionException {
			asyncAnalyzeAndTraverseStoryComments(comments);
			return "done task of batched comments" + comments.toString();
		}
	}
	private static void parsingCommentsTasksMaker(List<JSONArray> commentsList) throws InterruptedException, ExecutionException {
		ExecutorService service = Executors.newCachedThreadPool();
		List<Callable<String>> parsingCommentsTasks = new ArrayList<>();
		LOG.info("sending all parsing tasks to pool");
		for(JSONArray comments : commentsList){
			parsingCommentsTasks.add(new parseComments(comments));
		}
		LOG.info("done sending all parsing tasks to pool, now invoking them");
		List<Future<String>> results = service.invokeAll(parsingCommentsTasks);
		service.shutdown();
		LOG.info("done invoking all tasks");
		for(Future<String> result: results){
			LOG.info(result.get());
		}
	}
	private static void asyncAnalyzeStories(List<Integer> storiesToAnalyze) throws InterruptedException, ExecutionException {
		List<JSONArray> commentsByStory = HackerNewsAPI.asyncGetItemsComments(storiesToAnalyze);
		parsingCommentsTasksMaker(commentsByStory);
	}

	@Override
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
		LOG.info("received: {}", input);
		try{
			Map<String,String> quarryParameters =  (Map<String,String>) input.get("queryStringParameters");
			String phrase = quarryParameters.get("phrase");
			String decodedPhrase = URLDecoder.decode(phrase, StandardCharsets.UTF_8.toString());
			LOG.info("decode request: {}", decodedPhrase);
			LOG.info("start quering HackerNewsAPI..");

			List<Integer> topStoriesWithPhraseInTitle = HackerNewsAPI.asyncGetTopStoriesWithPhraseInTitle(decodedPhrase);
			LOG.info("start analyze stories");
			LOG.info("size of topstories: {}", topStoriesWithPhraseInTitle.size());
			if(!topStoriesWithPhraseInTitle.isEmpty())
				LOG.info("firststory: {}", topStoriesWithPhraseInTitle.get(0));
			asyncAnalyzeStories(topStoriesWithPhraseInTitle);
			LOG.info("finished analyze stories");


		} catch (Exception ex){
			LOG.error("error with path parameters: " + ex);
		}
		if(analyzer.alreadyAccumulatedResult()){
			return ApiGatewayResponse.builder()
					.setStatusCode(200)
					.setRawBody(analyzer.getResults().toString())
					.build();
		} else {
			return ApiGatewayResponse.builder()
					.setStatusCode(416)
					.build();
		}
	}
}
