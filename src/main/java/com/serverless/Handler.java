package com.serverless;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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

public class Handler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger LOG = LogManager.getLogger(Handler.class);

	private JSONObject getItem(long id) throws UnirestException {
		String url = "https://hacker-news.firebaseio.com/v0/item/" + id + ".json?print=pretty";
		HttpResponse<JsonNode> jsonResponse = Unirest.get(url).asJson();
		return jsonResponse.getBody().getObject();
	}

	private boolean containsPhraseCaseInsensitive(String title, String phrase){
		return Pattern.compile(Pattern.quote(phrase), Pattern.CASE_INSENSITIVE).matcher(title).find();
	}

	private List<Integer> getTopStoriesWithPhraseInTitle(String phrase) throws UnirestException {
		List<Integer> topStoriesWithPhraseInTitle = new ArrayList<>();
		String topStoriesUrl = "https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty";
		HttpResponse<JsonNode> jsonResponse = Unirest.get(topStoriesUrl).asJson();
		JSONArray jsonArray = jsonResponse.getBody().getArray();
		for(int i = 0; i < jsonArray.length(); ++i){
			int storyId = (int) jsonArray.get(i);
			JSONObject story = getItem(storyId);
			String title = story.getString("title");
			if(containsPhraseCaseInsensitive(title, phrase)){
				topStoriesWithPhraseInTitle.add(storyId);
			}
		}
		return topStoriesWithPhraseInTitle;
	}
	@Override
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
		LOG.info("received: {}", input);
		try{
			Map<String,String> pathParameters =  (Map<String,String>) input.get("pathParameters");
			String phrase = pathParameters.get("phrase");
			List<Integer> topStoriesWithPhraseInTitle = getTopStoriesWithPhraseInTitle(phrase);

			Response responseBody = new Response("Success: " + phrase, input);
			return ApiGatewayResponse.builder()
					.setStatusCode(200)
					.setObjectBody(responseBody)
					.setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & serverless"))
					.build();
		} catch (Exception ex){
			LOG.error("error with path parameters: " + ex);
		}
		return ApiGatewayResponse.builder()
				.setStatusCode(416)
				.setHeaders(Collections.singletonMap("X-Powered-By", "AWS Lambda & serverless"))
				.build();
	}
}
