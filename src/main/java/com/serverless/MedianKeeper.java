package com.serverless;

import java.util.PriorityQueue;
import java.util.Queue;

class MedianKeeper implements StreamAlgorithm{
    private Queue<Double> minHeap, maxHeap;

    public MedianKeeper() {
        this.minHeap = new PriorityQueue<>();
        this.maxHeap = new PriorityQueue<>();
    }
    @Override
    public void add(double num){
        if (!minHeap.isEmpty() && num < minHeap.peek()) {
            maxHeap.offer(num);
            if (maxHeap.size() > minHeap.size() + 1) {
                minHeap.offer(maxHeap.poll());
            }
        } else {
            minHeap.offer(num);
            if (minHeap.size() > maxHeap.size() + 1) {
                maxHeap.offer(minHeap.poll());
            }
        }
    }
    @Override
    public double get() {
        if(minHeap.isEmpty() || maxHeap.isEmpty()) return 0.0;
        double median;
        if (minHeap.size() < maxHeap.size()) {
            median = maxHeap.peek();
        } else if (minHeap.size() > maxHeap.size()) {
            median = minHeap.peek();
        } else {
            median = (minHeap.peek() + maxHeap.peek()) / 2;
        }
        return median;
    }
}
