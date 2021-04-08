package com.serverless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProcessStoriesByPhrase implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger LOG = LogManager.getLogger(ProcessStoriesByPhrase.class);
	private Analytics analyzer = new Analytics();

	/*
		traversing comments of story in a DFS manner, assumption is that comments child
		is only a comment and it contains text field.
	 */
	private void analyzeAndTraverseStoryComments(JSONArray comments) throws UnirestException {
		if(comments == null) return;
		for(int indexId = 0; indexId < comments.length(); ++indexId){
			int commentId = comments.getInt(indexId);
			String commentText = HackerNewsAPI.getCommentText(commentId);
			SentimentAnalysisAPI.getSentimentAnalysis(commentText);
			String attribute = SentimentAnalysisAPI.getAttribute();
			double ratio = SentimentAnalysisAPI.getRatio();
			double score = SentimentAnalysisAPI.getScore();
			analyzer.update(attribute, ratio, score);
			analyzeAndTraverseStoryComments(HackerNewsAPI.getItemComments(commentId));
		}
	}

	private void analyzeStories(List<Integer> storiesToAnalyze) throws UnirestException {
		for(int storyId : storiesToAnalyze){
			JSONArray comments = HackerNewsAPI.getItemComments(storyId);
			analyzeAndTraverseStoryComments(comments);
		}
	}
	@Override
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
		LOG.info("received: {}", input);
		try{
			Map<String,String> pathParameters =  (Map<String,String>) input.get("pathParameters");
			String phrase = pathParameters.get("phrase");
//			List<Integer> topStoriesWithPhraseInTitle = HackerNewsAPI.getTopStoriesWithPhraseInTitle(phrase);
			List<Integer> topStoriesWithPhraseInTitle = new ArrayList<>();
			topStoriesWithPhraseInTitle.add(8863);
			analyzeStories(topStoriesWithPhraseInTitle);

//			Response responseBody = new Response("Success: " + phrase, input);
			return ApiGatewayResponse.builder()
					.setStatusCode(200)
					.setRawBody(analyzer.getResults().toString())
//					.setObjectBody(responseBody)
					.setHeaders(Collections.singletonMap("Sentiment-Analysis-Powered-By", "AWS Lambda & serverless"))
					.build();
		} catch (Exception ex){
			LOG.error("error with path parameters: " + ex);
		}
		return ApiGatewayResponse.builder()
				.setStatusCode(416)
				.setRawBody(analyzer.getResults().toString())
				.setHeaders(Collections.singletonMap("Sentiment-Analysis-Powered-By", "AWS Lambda & serverless"))
				.build();
	}
}
