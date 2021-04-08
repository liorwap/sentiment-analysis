package com.serverless;

import org.json.JSONObject;
/*
 In order to change the class state, you must invoke getSentimentAnalysis method which calls
 sentiment analysis service via http request and updates the fields.
 */
public class SentimentAnalysisAPI {

    private static String attribute;
    private static double ratio;
    private static double score;

    public static void getSentimentAnalysis(String text){
        //TODO need to change class state
    }

    public static String getAttribute() {
        return attribute;
    }

    public static double getRatio() {
        return ratio;
    }

    public static double getScore() {
        return score;
    }
}
