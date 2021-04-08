package com.serverless;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/*
 In order to change the class state, you must invoke getSentimentAnalysis method which calls
 sentiment analysis service via http request and updates the fields.
 */
class SentimentAnalysisAPI {

    private static String attribute;
    private static double ratio;
    private static double score;
    private final static String BASIC_URL_API = "https://api.twinword.com/api/sentiment/analyze/latest/?text=";
    static void getSentimentAnalysis(String text) throws UnirestException {
        if(text.length() == 0) return; //waste api call
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
            System.out.println(encodedText);
            HttpResponse<JsonNode> response = Unirest.get(BASIC_URL_API + encodedText)
                    .header("Host", "api.twinword.com")
                    .header("X-Twaip-Key", "hJv+ZP5tRP6CQdO7qsP9Xgwu8hItC+w8Eqf6zRL3HY9ib57a2m/dDuBWgVo1xzd94I++7DcEsCdMyhZwiTHLIw==")
                    .asJson();
            JSONObject responseBody = response.getBody().getObject();
            attribute = responseBody.getString("type");
            score = responseBody.optDouble("score");
            ratio = responseBody.optDouble("ratio");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    static String getAttribute() {
        return attribute;
    }

    static double getRatio() {
        return ratio;
    }

    static double getScore() {
        return score;
    }
}
