package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.WarningLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class WarningService {

    @Value("${simulation.interval:10000}")
    private long interval;

    // berechnet die Warnstufe für eine Messung
    public WarningLevel calculateWarningLevel(Station station, Measurement measurement) {
        // keine oder ungültige Messung
        if (measurement == null || measurement.getWaterLevel() < 0) {
            return WarningLevel.UNKNOWN;
        }

        double waterLevel = measurement.getWaterLevel();

        if (waterLevel >= station.getCriticalWaterLevel()) {
            return WarningLevel.CRITICAL;
        }
        if (waterLevel >= station.getWarningWaterLevel()) {
            return WarningLevel.WARNING;
        }
        return WarningLevel.NORMAL;
    }

    // aktuelle Warnstufe einer Station, braucht man später für Filter und Alerts
    public WarningLevel getCurrentWarningLevel(Station station, Measurement latest) {
        if (latest == null) {
            return WarningLevel.UNKNOWN;
        }

        // wenn die letzte Messung älter als 3 Intervalle ist, gilt sie nicht mehr als aktuell
        Instant limit = Instant.now().minusMillis(interval * 3);
        if (latest.getTimestamp().isBefore(limit)) {
            return WarningLevel.UNKNOWN;
        }

        return calculateWarningLevel(station, latest);
    }
}