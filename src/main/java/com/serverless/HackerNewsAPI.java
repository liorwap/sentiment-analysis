package com.serverless;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

class HackerNewsAPI {
    private static final Logger LOG = LogManager.getLogger(HackerNewsAPI.class);
    private static final String BASE_URL_API = "https://hacker-news.firebaseio.com/v0";
    private static final String EXTENSION_URL_API = ".json?print=pretty";

    private static boolean containsPhraseCaseInsensitive(String title, String phrase){
        return Pattern.compile(Pattern.quote(phrase), Pattern.CASE_INSENSITIVE).matcher(title).find();
    }

    static JSONArray getTopStories() throws UnirestException {
        String topStoriesUrl = BASE_URL_API + "/topstories" + EXTENSION_URL_API;
        HttpResponse<JsonNode> jsonResponse = Unirest.get(topStoriesUrl).asJson();
        return jsonResponse.getBody().getArray();
    }
    static List<Integer> asyncGetTopStoriesWithPhraseInTitle(String phrase) throws UnirestException, InterruptedException, IOException {
        JSONArray topStories = getTopStories();
        List<Integer> topStoriesWithPhraseInTitle = new ArrayList<>();
        CountDownLatch responseWaiter = new CountDownLatch(topStories.length());
        for(int i = 0; i < topStories.length(); ++i){
            int storyId = (int) topStories.get(i);
            String url = BASE_URL_API + "/item/" + storyId + EXTENSION_URL_API;
            LOG.info("sending http request with storyId : {}", storyId);
            Future<HttpResponse<JsonNode>> request = Unirest.get(url).asJsonAsync(new Callback<JsonNode>() {
                @Override
                public void completed(HttpResponse<JsonNode> httpResponse) {
                    JSONObject story = httpResponse.getBody().getObject();
                    String title = story.getString("title");
                    LOG.info("completed http request, got title of story: {}", title);
                    if(containsPhraseCaseInsensitive(title, phrase)){
                        topStoriesWithPhraseInTitle.add(storyId);
                    }
                    responseWaiter.countDown();
                }

                @Override
                public void failed(UnirestException e) {
                    LOG.info(e.getMessage());
                    responseWaiter.countDown();
                }

                @Override
                public void cancelled() {
                    LOG.info("request cancelled");
                    responseWaiter.countDown();
                }
            });
        }
        responseWaiter.await();
        return topStoriesWithPhraseInTitle;
    }

    static JSONObject getItem(int id) throws UnirestException {
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
        return jsonObjResponse.has("kids") ? jsonObjResponse.getJSONArray("kids") : null;
    }
    static String getCommentText(int commentId) throws UnirestException {
        String commentUrl = BASE_URL_API + "/item/" + commentId + EXTENSION_URL_API;
        HttpResponse<JsonNode> jsonResponse = Unirest.get(commentUrl).asJson();
        JSONObject jsonObjResponse = jsonResponse.getBody().getObject();
        return jsonObjResponse.has("text") ? jsonObjResponse.getString("text") : null;

    }

    static List<JSONArray> asyncGetItemsComments(JSONArray storiesToAnalyze) throws InterruptedException{
        List<Integer> storiesToAnalyzeArray = new ArrayList<>();
        for(int i = 0; i < storiesToAnalyze.length(); ++i){
            storiesToAnalyzeArray.add(storiesToAnalyze.getInt(i));
        }
        return asyncGetItemsComments(storiesToAnalyzeArray);
    }

    static List<JSONArray> asyncGetItemsComments(List<Integer> storiesToAnalyze) throws InterruptedException {
        LOG.info("getting top level comments of stories async");
        List<JSONArray> itemsComments = new ArrayList<>();
        CountDownLatch responseWaiter = new CountDownLatch(storiesToAnalyze.size());
        for(int storyId: storiesToAnalyze){
            LOG.info("sending http request for comments from storyId : {}", storyId);
            String storyUrl = BASE_URL_API + "/item/" + storyId + EXTENSION_URL_API;
            Future<HttpResponse<JsonNode>> jsonResponse = Unirest.get(storyUrl).asJsonAsync(new Callback<JsonNode>() {
                @Override
                public void completed(HttpResponse<JsonNode> httpResponse) {
                    JSONObject jsonObjResponse = httpResponse.getBody().getObject();
                    if(jsonObjResponse.has("kids")){
                        itemsComments.add(jsonObjResponse.getJSONArray("kids"));
                        LOG.info("added comments of storyId: {}, comments are: {}",storyId, jsonObjResponse.getJSONArray("kids"));
                    }
                    responseWaiter.countDown();
                }

                @Override
                public void failed(UnirestException e) {
                    responseWaiter.countDown();
                }

                @Override
                public void cancelled() {
                    responseWaiter.countDown();
                }
            });

        }
        responseWaiter.await();
        return itemsComments;
    }

    public static List<String> asyncGetCommentsTexts(JSONArray comments) throws InterruptedException {
        LOG.info("getting comments texts");
        List<String> commentsTexts = new ArrayList<>();
        CountDownLatch responseWaiter = new CountDownLatch(comments.length());
        for(int i = 0; i < comments.length(); ++i){
            int commentId = comments.getInt(i);
            String commentUrl = BASE_URL_API + "/item/" + commentId + EXTENSION_URL_API;
            Future<HttpResponse<JsonNode>> jsonResponse = Unirest.get(commentUrl).asJsonAsync(new Callback<JsonNode>() {
                @Override
                public void completed(HttpResponse<JsonNode> httpResponse) {
                    JSONObject jsonObjResponse = httpResponse.getBody().getObject();
                    if(jsonObjResponse.has("text")){
                        String text = jsonObjResponse.getString("text");
                        LOG.info("got comment text: {}", text);
                        commentsTexts.add(text);
                    }
                    responseWaiter.countDown();
                }

                @Override
                public void failed(UnirestException e) {
                    responseWaiter.countDown();

                }

                @Override
                public void cancelled() {
                    responseWaiter.countDown();
                }
            });
        }
        responseWaiter.await();
        return commentsTexts;
    }
}
