package com.serverless;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/*
 In order to change the class state, you must invoke getSentimentAnalysis method which calls
 sentiment analysis service via http request and updates the fields.
 */
class SentimentAnalysisAPI {

    private static final Logger LOG = LogManager.getLogger(SentimentAnalysisAPI.class);

    public static class Analysis{
        private final String attribute;
        private final double ratio;
        private final double score;
        public Analysis(String attribute, double ratio, double score) {
            this.attribute = attribute;
            this.ratio = ratio;
            this.score = score;
        }
        public String getAttribute() {
            return attribute;
        }

        public double getRatio() {
            return ratio;
        }

        public double getScore() {
            return score;
        }
    }

    private final static String BASIC_URL_API = "https://api.twinword.com/api/sentiment/analyze/latest/?text=";
    static List<Analysis> asyncGetSentimentsAnalysis(List<String> texts) throws UnirestException, InterruptedException {
        LOG.info("begin analytics http requests for batched texts");
        List<Analysis> sentimentsAnalysis = new ArrayList<>();
        CountDownLatch responseWaiter = new CountDownLatch(texts.size());
        for(String text: texts){
            if(text.length() == 0) continue;
            try {
                String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
                LOG.info("sending text: {} encoded to: {}", text, encodedText);
                Future<HttpResponse<JsonNode>> response = Unirest.get(BASIC_URL_API + encodedText)
                        .header("Host",
                                "api.twinword.com")
                        .header("X-Twaip-Key",
                                "hJv+ZP5tRP6CQdO7qsP9Xgwu8hItC+w8Eqf6zRL3HY9ib57a2m/dDuBWgVo1xzd94I++7DcEsCdMyhZwiTHLIw==")
                        .asJsonAsync(new Callback<JsonNode>() {
                            @Override
                            public void completed(HttpResponse<JsonNode> httpResponse) {
                                LOG.info("completed analyze text: {}", text);
                                JSONObject responseBody = httpResponse.getBody().getObject();
                                String attribute = responseBody.getString("type");
                                double score = responseBody.optDouble("score");
                                double ratio = responseBody.optDouble("ratio");
                                sentimentsAnalysis.add(new Analysis(attribute, ratio, score));
                                LOG.info("added text to array of texts");
                                responseWaiter.countDown();
                            }

                            @Override
                            public void failed(UnirestException e) {
                                LOG.info("failed to recieve sentiment analysis");
                                responseWaiter.countDown();
                            }

                            @Override
                            public void cancelled() {
                                responseWaiter.countDown();
                            }
                        });
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        responseWaiter.await();
        return sentimentsAnalysis;
    }
}
