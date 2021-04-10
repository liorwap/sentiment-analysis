package com.serverless;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.HttpRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.JSONArray;

public class ProcessStoriesByPhrase implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger LOG = LogManager.getLogger(ProcessStoriesByPhrase.class);
	private static Analytics analyzer = new Analytics();
	private static final ReentrantLock lock = new ReentrantLock();

	/*
		traversing comments of story in a DFS manner, assumption is that comments child
		is only a comment and it contains text field.
	 */
//	private static void analyzeAndTraverseStoryComments(JSONArray comments) throws UnirestException {
//		if(comments == null) return;
//		for(int indexId = 0; indexId < comments.length(); ++indexId){
//			int commentId = comments.getInt(indexId);
//			String commentText = HackerNewsAPI.getCommentText(commentId);
//			SentimentAnalysisAPI.getSentimentAnalysis(commentText);
//			String attribute = SentimentAnalysisAPI.getAttribute();
//			double ratio = SentimentAnalysisAPI.getRatio();
//			double score = SentimentAnalysisAPI.getScore();
//			analyzer.update(attribute, ratio, score);
//			analyzeAndTraverseStoryComments(HackerNewsAPI.getItemComments(commentId));
//		}
//	}
	private static void asyncAnalyzeAndTraverseStoryComments(JSONArray comments) throws UnirestException, InterruptedException {
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
		for(JSONArray moreComments : lowerLevelComments){
			asyncAnalyzeAndTraverseStoryComments(moreComments);
		}
	}
	private static void asyncAnalyzeStories(List<Integer> storiesToAnalyze) throws UnirestException, InterruptedException {
		List<JSONArray> commentsByStory = HackerNewsAPI.asyncGetItemsComments(storiesToAnalyze);
		for(JSONArray comments : commentsByStory){
			asyncAnalyzeAndTraverseStoryComments(comments);
		}
	}
//	private void analyzeStories(List<Integer> storiesToAnalyze) throws UnirestException {
//		for(int storyId : storiesToAnalyze){
//			JSONArray comments = HackerNewsAPI.getItemComments(storyId);
//			analyzeAndTraverseStoryComments(comments);
//		}
//	}

//	public static void main(String[] args) throws IOException, InterruptedException, UnirestException {
//		String decodedPhrase = URLDecoder.decode("GRAPH", StandardCharsets.UTF_8.toString());
//		List<Integer> topStoriesWithPhraseInTitle = HackerNewsAPI.asyncGetTopStoriesWithPhraseInTitle(decodedPhrase);
//		LOG.info("start analyze stories");
//		LOG.info("size of topstories: {}", topStoriesWithPhraseInTitle.size());
//		if(!topStoriesWithPhraseInTitle.isEmpty())
//			LOG.info("firststory: {}", topStoriesWithPhraseInTitle.get(0));
//		asyncAnalyzeStories(topStoriesWithPhraseInTitle);
//		LOG.info("finished analyze stories");
//		LOG.info("RESULT IS :L {}", analyzer.getResults().toString());
//		Unirest.shutdown();
//	}
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
