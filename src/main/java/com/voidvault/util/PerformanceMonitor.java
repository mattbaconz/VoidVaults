package com.voidvault.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple performance monitoring utility for tracking operation metrics.
 * Tracks operation counts, total time, and calculates averages.
 */
public class PerformanceMonitor {
    
    private static final Map<String, OperationMetrics> metrics = new ConcurrentHashMap<>();
    
    /**
     * Records the execution time of an operation.
     *
     * @param operationName The name of the operation
     * @param durationMs    The duration in milliseconds
     */
    public static void recordOperation(String operationName, long durationMs) {
        metrics.computeIfAbsent(operationName, k -> new OperationMetrics())
               .record(durationMs);
    }
    
    /**
     * Gets metrics for a specific operation.
     *
     * @param operationName The name of the operation
     * @return The metrics, or null if no data exists
     */
    public static OperationMetrics getMetrics(String operationName) {
        return metrics.get(operationName);
    }
    
    /**
     * Gets all tracked metrics.
     *
     * @return A map of operation names to their metrics
     */
    public static Map<String, OperationMetrics> getAllMetrics() {
        return Map.copyOf(metrics);
    }
    
    /**
     * Clears all metrics.
     */
    public static void reset() {
        metrics.clear();
    }
    
    /**
     * Clears metrics for a specific operation.
     *
     * @param operationName The name of the operation
     */
    public static void reset(String operationName) {
        metrics.remove(operationName);
    }
    
    /**
     * Formats all metrics as a human-readable string.
     *
     * @return Formatted metrics string
     */
    public static String formatAllMetrics() {
        if (metrics.isEmpty()) {
            return "No performance data available";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Performance Metrics:\n");
        
        metrics.forEach((name, metric) -> {
            sb.append(String.format("  %s: %s\n", name, metric.toString()));
        });
        
        return sb.toString();
    }
    
    /**
     * Tracks metrics for a single operation type.
     */
    public static class OperationMetrics {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTime = new AtomicLong(0);
        
        /**
         * Records a single operation execution.
         *
         * @param durationMs The duration in milliseconds
         */
        public void record(long durationMs) {
            count.incrementAndGet();
            totalTime.addAndGet(durationMs);
            
            // Update min
            long currentMin;
            do {
                currentMin = minTime.get();
                if (durationMs >= currentMin) break;
            } while (!minTime.compareAndSet(currentMin, durationMs));
            
            // Update max
            long currentMax;
            do {
                currentMax = maxTime.get();
                if (durationMs <= currentMax) break;
            } while (!maxTime.compareAndSet(currentMax, durationMs));
        }
        
        /**
         * Gets the total number of operations recorded.
         *
         * @return The operation count
         */
        public long getCount() {
            return count.get();
        }
        
        /**
         * Gets the total time spent on all operations.
         *
         * @return The total time in milliseconds
         */
        public long getTotalTime() {
            return totalTime.get();
        }
        
        /**
         * Gets the average time per operation.
         *
         * @return The average time in milliseconds
         */
        public double getAverageTime() {
            long c = count.get();
            return c > 0 ? (double) totalTime.get() / c : 0.0;
        }
        
        /**
         * Gets the minimum operation time.
         *
         * @return The minimum time in milliseconds
         */
        public long getMinTime() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        /**
         * Gets the maximum operation time.
         *
         * @return The maximum time in milliseconds
         */
        public long getMaxTime() {
            return maxTime.get();
        }
        
        @Override
        public String toString() {
            return String.format("Count=%d, Avg=%.2fms, Min=%dms, Max=%dms, Total=%dms",
                getCount(), getAverageTime(), getMinTime(), getMaxTime(), getTotalTime());
        }
    }
}
