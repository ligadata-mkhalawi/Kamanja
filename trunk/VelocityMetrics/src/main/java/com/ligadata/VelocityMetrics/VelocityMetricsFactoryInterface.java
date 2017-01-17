package com.ligadata.VelocityMetrics;

public interface VelocityMetricsFactoryInterface {
    VelocityMetricsInstanceInterface GetVelocityMetricsInstance(String nodeId, String componentKey, long intervalRoundingInMs, String[] countersNames);

    void shutdown();

    void addEmitListener(VelocityMetricsCallback velocityMetricsCallback);
}
