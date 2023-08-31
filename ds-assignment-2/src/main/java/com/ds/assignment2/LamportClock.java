package com.ds.assignment2;

public class LamportClock {
    private int value = 0;
    
    public synchronized void tick() {
        value++;
    }

    public synchronized int getValue() {
        return value;
    }

    public synchronized void updateValue(int newValue) {
        value = Math.max(value, newValue);
    }
}
