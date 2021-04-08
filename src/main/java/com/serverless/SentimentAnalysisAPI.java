package com.serverless;

import org.json.JSONObject;
/*
 In order to change the class state, you must invoke getSentimentAnalysis method which calls
 sentiment analysis service via http request and updates the fields.
 */
class SentimentAnalysisAPI {

    private static String attribute;
    private static double ratio;
    private static double score;

    static void getSentimentAnalysis(String text){
        //TODO need to change class state
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
