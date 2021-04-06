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
import com.sun.tools.javac.util.Log;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import org.json.JSONObject;

public class Handler implements RequestHandler<Map<String, Object>, ApiGatewayResponse> {

	private static final Logger LOG = LogManager.getLogger(Handler.class);

	private long getMaxItemId() throws UnirestException {
		HttpResponse<String> response =
				Unirest.get("https://hacker-news.firebaseio.com/v0/maxitem.json?print=pretty")
						.asString();
		String body = response.getBody().replace("\n", "");
		return Long.parseLong(body);
	}

	private JSONObject getItem(long id) throws UnirestException {
		String url = String.format("https://hacker-news.firebaseio.com/v0/item/%o.json?print=pretty", id);
		HttpResponse<JsonNode> jsonResponse = Unirest.get(url).asJson();
		return jsonResponse.getBody().getObject();
	}

	private boolean containsPhraseCaseInsensitive(String title, String phrase){
		return Pattern.compile(Pattern.quote(phrase), Pattern.CASE_INSENSITIVE).matcher(title).find();
	}

	private boolean isStoryWithPhraseInTitle(JSONObject responseObj, String phrase){
		if(responseObj == null) return false;
		if(!responseObj.getString("type").equals("story")) return false;
 		return containsPhraseCaseInsensitive(responseObj.getString("title"), phrase);
	}

	private List<Long> getStoriesId(String phrase) throws UnirestException {
		List<Long> idsByTitle = new ArrayList<>();
		long maxId = getMaxItemId();
		for(long itemId = 0L; itemId <= maxId; ++itemId){
			JSONObject responseObj = getItem(itemId);
			if(isStoryWithPhraseInTitle(responseObj, phrase)){
				idsByTitle.add(itemId);
			}
		}
		return idsByTitle;
	}
	@Override
	public ApiGatewayResponse handleRequest(Map<String, Object> input, Context context) {
		LOG.info("received: {}", input);
		try{
			Map<String,String> pathParameters =  (Map<String,String>) input.get("pathParameters");
			String phrase = pathParameters.get("phrase");
			List<Long> storiesId = getStoriesId(phrase);
			for(long storyId: storiesId){
				LOG.info(storyId);
			}
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
