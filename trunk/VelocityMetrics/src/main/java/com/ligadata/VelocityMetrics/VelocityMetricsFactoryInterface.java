package com.ligadata.VelocityMetrics;

public interface VelocityMetricsFactoryInterface {
    VelocityMetricsInstanceInterface GetVelocityMetricsInstance(String nodeId, String componentKey, int intervalRoundingInSec, String[] countersNames);

    void shutdown();

    void addEmitListener(VelocityMetricsCallback velocityMetricsCallback);
}
