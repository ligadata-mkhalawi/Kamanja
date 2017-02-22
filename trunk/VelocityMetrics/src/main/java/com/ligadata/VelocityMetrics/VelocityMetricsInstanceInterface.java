package com.ligadata.VelocityMetrics;

public interface VelocityMetricsInstanceInterface {
    // Adds counter for each flag where ever index says TRUE
    // Like if addCntrIdxFlags value is true, false, true, true, false, true, then it adds counter for indices 0, 2, 3, 5
    void increment(long metricsTime, String key, long currentTime, boolean... addCntrIdxFlags);

    // Simply increment counters for given indices.
    void increment(long metricsTime, String key, long currentTime, int[] addCntrIdxs);

    // Simply add counters for given indices with the given values
    void Add(long metricsTime, String key, long currentTime, int[] addCntrIdxs, int[] addCntrValuesForIdxs);

    // Duplicate VelocityMetricsInstanceInterface
    VelocityMetricsInstanceInterface duplicate();

    // Add current VelocityMetricsInstanceInterface to given VelocityMetricsInstanceInterface
    void addTo(VelocityMetricsInstanceInterface metrics, boolean reset);
}
