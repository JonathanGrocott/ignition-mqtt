package com.inductiveautomation.ignition.examples.mqtt.sparkplug.gateway;

import java.util.concurrent.atomic.AtomicLong;

public class SparkplugBdSeqManager {

    private final AtomicLong bdSeq = new AtomicLong(0);

    public long current() {
        return bdSeq.get();
    }

    public long next() {
        return bdSeq.updateAndGet(current -> (current + 1) & 0xFF);
    }
}
