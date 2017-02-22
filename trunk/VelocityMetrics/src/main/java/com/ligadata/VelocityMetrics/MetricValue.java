package com.ligadata.VelocityMetrics;

public class MetricValue {
    private String metricKey = null;
    private long metricValue = 0L;

    public String Key() {
        return metricKey;
    }

    public long Value() {
        return metricValue;
    }

    public MetricValue(String key, long value) {
        metricKey = key;
        metricValue = value;
    }
}
