package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Measurement;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class MeasurementRepository {

    // Key = stationId, Value = alle Messungen dieser Station
    // Concurrent-Varianten, weil die Simulation in einem eigenen Thread läuft
    private Map<String, List<Measurement>> measurements = new ConcurrentHashMap<>();

    public void save(Measurement measurement) {
        String stationId = measurement.getStationId();
        if (!measurements.containsKey(stationId)) {
            measurements.put(stationId, new CopyOnWriteArrayList<>());
        }
        measurements.get(stationId).add(measurement);
    }

    public List<Measurement> findByStationId(String stationId) {
        if (measurements.containsKey(stationId)) {
            return measurements.get(stationId);
        }
        return new ArrayList<>();
    }

    public Measurement findLatest(String stationId) {
        List<Measurement> list = findByStationId(stationId);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    // wird für die Tests gebraucht
    public void deleteAll() {
        measurements.clear();
    }
}