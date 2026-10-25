package com.example.floodmonitor.model;

public class Statistics {

    private String stationId;
    private double minWaterLevel;
    private double maxWaterLevel;
    private double averageWaterLevel;
    private double averageFlowRate;
    private double totalRainfall;
    private int measurementCount;
    private int warningCount;   // WARNING + CRITICAL
    private int criticalCount;  // nur CRITICAL

    public Statistics() {
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public double getMinWaterLevel() {
        return minWaterLevel;
    }

    public void setMinWaterLevel(double minWaterLevel) {
        this.minWaterLevel = minWaterLevel;
    }

    public double getMaxWaterLevel() {
        return maxWaterLevel;
    }

    public void setMaxWaterLevel(double maxWaterLevel) {
        this.maxWaterLevel = maxWaterLevel;
    }

    public double getAverageWaterLevel() {
        return averageWaterLevel;
    }

    public void setAverageWaterLevel(double averageWaterLevel) {
        this.averageWaterLevel = averageWaterLevel;
    }

    public double getAverageFlowRate() {
        return averageFlowRate;
    }

    public void setAverageFlowRate(double averageFlowRate) {
        this.averageFlowRate = averageFlowRate;
    }

    public double getTotalRainfall() {
        return totalRainfall;
    }

    public void setTotalRainfall(double totalRainfall) {
        this.totalRainfall = totalRainfall;
    }

    public int getMeasurementCount() {
        return measurementCount;
    }

    public void setMeasurementCount(int measurementCount) {
        this.measurementCount = measurementCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getCriticalCount() {
        return criticalCount;
    }

    public void setCriticalCount(int criticalCount) {
        this.criticalCount = criticalCount;
    }
}