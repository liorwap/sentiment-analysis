package com.serverless;

public class AverageKeeper implements StreamAlgorithm {
    private double sum;
    private int n;

    public AverageKeeper() {
        this.sum = 0.0;
        this.n = 0;
    }

    @Override
    public void add(double num) {
        sum += num;
        n++;
    }

    @Override
    public double get() {
        if(n == 0) return 0.0;
        return sum / n;
    }
}
