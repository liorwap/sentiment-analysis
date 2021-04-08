package com.serverless;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class HackerNewsAPI {
    private static final String BASE_URL_API = "https://hacker-news.firebaseio.com/v0";
    private static final String EXTENSION_URL_API = ".json?print=pretty";

    private static boolean containsPhraseCaseInsensitive(String title, String phrase){
        return Pattern.compile(Pattern.quote(phrase), Pattern.CASE_INSENSITIVE).matcher(title).find();
    }

    static JSONObject getItem(long id) throws UnirestException {
        String url = BASE_URL_API + "/item/" + id + EXTENSION_URL_API;
        HttpResponse<JsonNode> jsonResponse = Unirest.get(url).asJson();
        return jsonResponse.getBody().getObject();
    }
    static List<Integer> getTopStoriesWithPhraseInTitle(String phrase) throws UnirestException {
        List<Integer> topStoriesWithPhraseInTitle = new ArrayList<>();
        String topStoriesUrl = BASE_URL_API + "/topstories" + EXTENSION_URL_API;
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

    /*
     must contain storyId or commentId
     */
    static JSONArray getItemComments(int itemId) throws UnirestException {
        String storyUrl = BASE_URL_API + "/item/" + itemId + EXTENSION_URL_API;
        HttpResponse<JsonNode> jsonResponse = Unirest.get(storyUrl).asJson();
        JSONObject jsonObjResponse = jsonResponse.getBody().getObject();
        return jsonObjResponse.getJSONArray("kids");
    }
    static String getCommentText(int commentId) throws UnirestException {
        String commentUrl = BASE_URL_API + "/item/" + commentId + EXTENSION_URL_API;
        HttpResponse<JsonNode> jsonResponse = Unirest.get(commentUrl).asJson();
        JSONObject jsonObjResponse = jsonResponse.getBody().getObject();
        return jsonObjResponse.getString("text");

    }
}
