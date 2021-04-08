package com.serverless;

import org.json.JSONObject;

class Analytics {
    private final String POSITIVE = "positive";
    private final String NEUTRAL = "neutral";
    private final String NEGATIVE = "negative";
    private final String MIXED = "mixed";
    private final String[] attributes = {POSITIVE, NEUTRAL, NEGATIVE, MIXED};
    private int numOfCallsToUpdate = 0;
    private AverageKeeper[] averages = new AverageKeeper[attributes.length];
    private MedianKeeper[] medians = new MedianKeeper[attributes.length];

    private final double ratioEpsilon = 0.05;

    void update(String attribute, double ratio, double value){
        numOfCallsToUpdate++;
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

    private void updatePositive(double value) {
        averages[0].add(value);
        medians[0].add(value);
    }

    private void updateNeutral(double value) {
        averages[1].add(value);
        medians[1].add(value);
    }

    private void updateNegative(double value) {
        averages[2].add(value);
        medians[2].add(value);
    }

    private void updateMixed(double value) {
        averages[3].add(value);
        medians[3].add(value);
    }

    JSONObject getResults() {
        JSONObject analyticsScheme = new JSONObject();
        analyticsScheme.put("comments", numOfCallsToUpdate);
        int i = 0;
        for(String attribute: attributes) {
            JSONObject resultHolder = new JSONObject();
            resultHolder.put("avg", averages[i].get());
            resultHolder.put("median", medians[i].get());
            analyticsScheme.put(attribute, resultHolder);
            ++i;
        }
        return analyticsScheme;}
}
