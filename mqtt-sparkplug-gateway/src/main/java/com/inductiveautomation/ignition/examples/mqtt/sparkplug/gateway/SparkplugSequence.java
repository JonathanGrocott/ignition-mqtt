package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway;

import java.util.concurrent.atomic.AtomicInteger;

public class SparkplugSequence {

    private final AtomicInteger sequence = new AtomicInteger(0);

    public int next() {
        return sequence.updateAndGet(current -> (current + 1) & 0xFF);
    }

    public int current() {
        return sequence.get();
    }

    public void reset() {
        sequence.set(0);
    }
}
