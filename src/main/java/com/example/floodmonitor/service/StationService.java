package com.example.floodmonitor.service;

import com.example.floodmonitor.exception.StationNotFoundException;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.Statistics;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class StationService {

    public static final int MAX_LIMIT = 1000;

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;

    @Value("${simulation.interval:10000}")
    private long interval;

    public StationService(StationRepository stationRepository, MeasurementRepository measurementRepository) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
    }

    // ---------- Stationen ----------

    // alle Stationen, optional gefiltert (null = Filter nicht gesetzt)
    public List<Station> getStations(String river, Boolean isActive, WarningLevel warningLevel) {
        List<Station> result = new ArrayList<>();

        for (Station station : stationRepository.findAll()) {
            if (river != null && !station.getRiver().equalsIgnoreCase(river)) {
                continue;
            }
            if (isActive != null && station.isActive() != isActive) {
                continue;
            }
            if (warningLevel != null && getCurrentWarningLevel(station) != warningLevel) {
                continue;
            }
            result.add(station);
        }

        return result;
    }

    public Station getStation(String stationId) {
        Station station = stationRepository.findById(stationId);
        if (station == null) {
            throw new StationNotFoundException(stationId);
        }
        return station;
    }

    // ---------- Warnstufe ----------

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

    // aktuelle Warnstufe einer Station
    public WarningLevel getCurrentWarningLevel(Station station) {
        Measurement latest = measurementRepository.findLatest(station.getId());
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

    // ---------- Messwerte ----------

    public Measurement getLatestMeasurement(String stationId) {
        getStation(stationId); // wirft Exception, wenn die Station nicht existiert
        return measurementRepository.findLatest(stationId);
    }

    // historische Messwerte, neueste zuerst
    public List<Measurement> getMeasurements(String stationId, Instant from, Instant to,
                                             int limit, WarningLevel warningLevel) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit muss mindestens 1 sein");
        }
        if (limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit darf maximal " + MAX_LIMIT + " sein");
        }

        return filterMeasurements(stationId, from, to, limit, warningLevel);
    }

    private List<Measurement> filterMeasurements(String stationId, Instant from, Instant to,
                                                 int limit, WarningLevel warningLevel) {
        getStation(stationId);

        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("Ungültiger Zeitraum: from darf nicht nach to liegen");
        }

        List<Measurement> all = measurementRepository.findByStationId(stationId);
        List<Measurement> result = new ArrayList<>();

        // von hinten durchgehen, damit die neuesten Messungen zuerst kommen
        for (int i = all.size() - 1; i >= 0; i--) {
            Measurement measurement = all.get(i);

            if (from != null && measurement.getTimestamp().isBefore(from)) {
                continue;
            }
            if (to != null && measurement.getTimestamp().isAfter(to)) {
                continue;
            }
            if (warningLevel != null && measurement.getWarningLevel() != warningLevel) {
                continue;
            }

            result.add(measurement);

            if (result.size() >= limit) {
                break;
            }
        }

        return result;
    }

    // ---------- Statistik ----------

    public Statistics getStatistics(String stationId, Instant from, Instant to) {
        // für die Statistik alle Messungen im Zeitraum verwenden (kein limit)
        List<Measurement> list = filterMeasurements(stationId, from, to, Integer.MAX_VALUE, null);

        Statistics statistics = new Statistics();
        statistics.setStationId(stationId);
        statistics.setMeasurementCount(list.size());

        if (list.isEmpty()) {
            return statistics;
        }

        double min = list.get(0).getWaterLevel();
        double max = list.get(0).getWaterLevel();
        double sumWaterLevel = 0;
        double sumFlowRate = 0;
        double sumRainfall = 0;
        int warnings = 0;
        int critical = 0;

        for (Measurement measurement : list) {
            if (measurement.getWaterLevel() < min) {
                min = measurement.getWaterLevel();
            }
            if (measurement.getWaterLevel() > max) {
                max = measurement.getWaterLevel();
            }
            sumWaterLevel += measurement.getWaterLevel();
            sumFlowRate += measurement.getFlowRate();
            sumRainfall += measurement.getRainfall();

            if (measurement.getWarningLevel() == WarningLevel.WARNING
                    || measurement.getWarningLevel() == WarningLevel.CRITICAL) {
                warnings++;
            }
            if (measurement.getWarningLevel() == WarningLevel.CRITICAL) {
                critical++;
            }
        }

        statistics.setMinWaterLevel(min);
        statistics.setMaxWaterLevel(max);
        statistics.setAverageWaterLevel(round(sumWaterLevel / list.size()));
        statistics.setAverageFlowRate(round(sumFlowRate / list.size()));
        statistics.setTotalRainfall(round(sumRainfall));
        statistics.setWarningCount(warnings);
        statistics.setCriticalCount(critical);

        return statistics;
    }

    // ---------- Warnungen (Alerts) ----------

    // aktuelle Warnungen = letzte Messung aller Stationen mit WARNING oder CRITICAL
    public List<Measurement> getAlerts(WarningLevel warningLevel, String river) {
        List<Measurement> alerts = new ArrayList<>();

        for (Station station : stationRepository.findAll()) {
            if (river != null && !station.getRiver().equalsIgnoreCase(river)) {
                continue;
            }

            WarningLevel level = getCurrentWarningLevel(station);

            if (level != WarningLevel.WARNING && level != WarningLevel.CRITICAL) {
                continue;
            }
            if (warningLevel != null && level != warningLevel) {
                continue;
            }

            alerts.add(measurementRepository.findLatest(station.getId()));
        }

        return alerts;
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}