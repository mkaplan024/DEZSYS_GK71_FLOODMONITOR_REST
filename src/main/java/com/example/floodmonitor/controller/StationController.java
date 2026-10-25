package com.example.floodmonitor.controller;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.Statistics;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.service.StationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @GetMapping("/stations")
    public List<Station> getStations(@RequestParam(required = false) String river,
                                     @RequestParam(required = false) Boolean isActive,
                                     @RequestParam(required = false) WarningLevel warningLevel) {
        return stationService.getStations(river, isActive, warningLevel);
    }

    @GetMapping("/stations/{stationId}")
    public Station getStation(@PathVariable String stationId) {
        return stationService.getStation(stationId);
    }

    @GetMapping("/stations/{stationId}/measurements/latest")
    public ResponseEntity<Measurement> getLatestMeasurement(@PathVariable String stationId) {
        Measurement latest = stationService.getLatestMeasurement(stationId);
        if (latest == null) {
            // Station gibt es, aber noch keine Messung -> 204 No Content
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }

    @GetMapping("/stations/{stationId}/measurements")
    public List<Measurement> getMeasurements(@PathVariable String stationId,
                                             @RequestParam(required = false) Instant from,
                                             @RequestParam(required = false) Instant to,
                                             @RequestParam(defaultValue = "100") int limit,
                                             @RequestParam(required = false) WarningLevel warningLevel) {
        return stationService.getMeasurements(stationId, from, to, limit, warningLevel);
    }

    @GetMapping("/stations/{stationId}/statistics")
    public Statistics getStatistics(@PathVariable String stationId,
                                    @RequestParam(required = false) Instant from,
                                    @RequestParam(required = false) Instant to) {
        return stationService.getStatistics(stationId, from, to);
    }

    @GetMapping("/alerts")
    public List<Measurement> getAlerts(@RequestParam(required = false) WarningLevel warningLevel,
                                       @RequestParam(required = false) String river) {
        return stationService.getAlerts(warningLevel, river);
    }
}