package com.serverless;

import org.json.JSONObject;

import java.math.BigDecimal;
import java.math.RoundingMode;

class Analytics {
    private final String POSITIVE = "positive";
    private final String NEUTRAL = "neutral";
    private final String NEGATIVE = "negative";
    private final String MIXED = "mixed";
    private final String[] attributes = {POSITIVE, NEUTRAL, NEGATIVE, MIXED};
    private int numOfCallsToUpdate = 0;
    private final AverageKeeper[] averages = new AverageKeeper[attributes.length];
    private final MedianKeeper[] medians = new MedianKeeper[attributes.length];

    private final double ratioEpsilon = 0.05;

    public Analytics() {
        for(int i = 0; i < attributes.length; ++i){
            averages[i] = new AverageKeeper();
            medians[i] = new MedianKeeper();
        }
    }
    boolean alreadyAccumulatedResult(){
        return numOfCallsToUpdate > 0;
    }
    /*
    ratioEpsilon used here, it is the threshold of deciding if a neutral text is actually contains
    about the same amount of both positive and negative keywords.
     */
    void update(String attribute, double ratio, double score){
        numOfCallsToUpdate++;
        score = Math.abs(score);
        if(attribute.equals(NEUTRAL) && Math.abs(1 - ratio) <= ratioEpsilon){
            attribute = MIXED;
        }
        switch (attribute){
            case POSITIVE:
                updatePositive(score);
                break;
            case NEUTRAL:
                updateNeutral(score);
                break;
            case NEGATIVE:
                updateNegative(score);
                break;
            case MIXED:
                updateMixed(score);
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
            BigDecimal average = BigDecimal.valueOf(averages[i].get()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal median = BigDecimal.valueOf(medians[i].get()).setScale(2, RoundingMode.HALF_UP);
            resultHolder.put("avg", average.doubleValue());
            resultHolder.put("median", median);
            analyticsScheme.put(attribute, resultHolder);
            ++i;
        }
        return analyticsScheme;}
}
