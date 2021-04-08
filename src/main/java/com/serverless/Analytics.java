package com.serverless;

import org.json.JSONObject;

public class Analytics {
    private final String POSITIVE = "positive";
    private final String NEUTRAL = "neutral";
    private final String NEGATIVE = "negative";
    private final String MIXED = "mixed";
    private final String[] attributes = {POSITIVE, NEUTRAL, NEGATIVE, MIXED};
    private final double ratioEpsilon = 0.05;
    private JSONObject results;

    public Analytics(){
        this.results = buildAnalyticsSchemeTemplate();
    }

    private JSONObject buildAnalyticsSchemeTemplate(){
        JSONObject analyticsScheme = new JSONObject();
        analyticsScheme.put("comments", 0);
        for(String attribute: attributes) {
            JSONObject resultHolder = new JSONObject();
            resultHolder.put("avg", 0.0);
            resultHolder.put("median", 0.0);
            analyticsScheme.put(attribute, resultHolder);
        }
        return analyticsScheme;
    }

    public void update(String attribute, double ratio, double value){
        if(attribute.equals(NEUTRAL) && Math.abs(ratio) <= ratioEpsilon){
            attribute = MIXED;
        }
        switch (attribute){
            case POSITIVE:
                updatePositive(value);
                break;
            case NEUTRAL:
                updateNeutral(value);
                break;
            case NEGATIVE:
                updateNegative(value);
                break;
            case MIXED:
                updateMixed(value);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + attribute);
        }
    }

    private void updateMixed(double value) {

    }

    private void updateNegative(double value) {

    }

    private void updateNeutral(double value) {

    }

    private void updatePositive(double value) {

    }

    public JSONObject getResults() {
        return results;
    }


}
