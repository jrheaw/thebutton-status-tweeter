package com.wittenauer;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by jwittenauer on 4/2/15.
 *
 * See {@linktourl http://rosettacode.org/wiki/Averages/Simple_moving_average}
 */
public class MovingAverage {
    private final Queue<Double> window = new LinkedList<Double>();
    private final int period;
    private double sum;

    public MovingAverage(int period) {
        assert period > 0 : "Period must be a positive integer";
        this.period = period;
    }

    public void newNum(double num) {
        sum += num;
        window.add(num);
        if (window.size() > period) {
            sum -= window.remove();
        }
    }

    public double getAvg() {
        if (window.isEmpty()) return 0; // technically the average is undefined
        return sum / window.size();
    }
}
