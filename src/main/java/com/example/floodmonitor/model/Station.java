package com.example.floodmonitor.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Station {

    private String id;
    private String name;
    private String river;
    private Location location;
    private double normalWaterLevel;    // in cm
    private double warningWaterLevel;   // in cm, ab hier WARNING
    private double criticalWaterLevel;  // in cm, ab hier CRITICAL
    private boolean active;
    private StationStatus status;

    public Station() {
    }

    public Station(String id, String name, String river, Location location,
                   double normalWaterLevel, double warningWaterLevel, double criticalWaterLevel,
                   boolean active, StationStatus status) {
        this.id = id;
        this.name = name;
        this.river = river;
        this.location = location;
        this.normalWaterLevel = normalWaterLevel;
        this.warningWaterLevel = warningWaterLevel;
        this.criticalWaterLevel = criticalWaterLevel;
        this.active = active;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRiver() {
        return river;
    }

    public void setRiver(String river) {
        this.river = river;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double getNormalWaterLevel() {
        return normalWaterLevel;
    }

    public void setNormalWaterLevel(double normalWaterLevel) {
        this.normalWaterLevel = normalWaterLevel;
    }

    public double getWarningWaterLevel() {
        return warningWaterLevel;
    }

    public void setWarningWaterLevel(double warningWaterLevel) {
        this.warningWaterLevel = warningWaterLevel;
    }

    public double getCriticalWaterLevel() {
        return criticalWaterLevel;
    }

    public void setCriticalWaterLevel(double criticalWaterLevel) {
        this.criticalWaterLevel = criticalWaterLevel;
    }

    // ohne @JsonProperty würde im JSON "active" statt "isActive" stehen
    @JsonProperty("isActive")
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        this.status = status;
    }
}