package com.example.floodmonitor.service;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.repository.StationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Service
public class SimulationService {

    private final StationRepository stationRepository;
    private final MeasurementRepository measurementRepository;
    private final StationService stationService;
    private final Random random = new Random();

    @Value("${simulation.enabled:true}")
    private boolean enabled;

    public SimulationService(StationRepository stationRepository, MeasurementRepository measurementRepository,
                             StationService stationService) {
        this.stationRepository = stationRepository;
        this.measurementRepository = measurementRepository;
        this.stationService = stationService;
    }

    // wird automatisch alle "simulation.interval" Millisekunden aufgerufen
    @Scheduled(fixedRateString = "${simulation.interval:10000}")
    public void simulate() {
        if (!enabled) {
            return;
        }

        for (Station station : stationRepository.findAll()) {
            // für deaktivierte oder ausgefallene Stationen keine Messungen
            if (!station.isActive() || station.getStatus() == StationStatus.OFFLINE) {
                continue;
            }

            Measurement previous = measurementRepository.findLatest(station.getId());
            Measurement measurement = createMeasurement(station, previous);
            measurementRepository.save(measurement);

            System.out.println("Neue Messung " + station.getId()
                    + ": Wasserstand=" + measurement.getWaterLevel()
                    + " cm, Regen=" + measurement.getRainfall()
                    + " mm/h, Durchfluss=" + measurement.getFlowRate()
                    + " m3/s, Akku=" + measurement.getBatteryLevel()
                    + " %, Status=" + measurement.getStatus()
                    + ", Warnstufe=" + measurement.getWarningLevel());
        }
    }

    public Measurement createMeasurement(Station station, Measurement previous) {
        double waterLevel;
        double rainfall;
        double temperature;
        int batteryLevel;

        if (previous == null) {
            // erste Messung: Startwerte
            waterLevel = station.getNormalWaterLevel();
            rainfall = 0;
            temperature = 12;
            batteryLevel = 50 + random.nextInt(51); // 50 bis 100
        } else {
            // Niederschlag ändert sich pro Messung um höchstens 3 mm/h
            rainfall = previous.getRainfall() + (random.nextDouble() * 6 - 3);
            if (rainfall < 0) {
                rainfall = 0;
            }
            if (rainfall > 30) {
                rainfall = 30;
            }

            // Regen lässt den Wasserstand steigen,
            // ohne Regen geht er langsam wieder Richtung Normalwert zurück
            double change = rainfall * 0.8
                    - (previous.getWaterLevel() - station.getNormalWaterLevel()) * 0.05
                    + (random.nextDouble() * 4 - 2);
            waterLevel = previous.getWaterLevel() + change;

            // Temperatur ändert sich nur leicht
            temperature = previous.getTemperature() + (random.nextDouble() * 0.6 - 0.3);

            // Akku wird pro Messung um 0 oder 1 Prozent leerer
            batteryLevel = previous.getBatteryLevel() - random.nextInt(2);
        }

        // Wasserstand darf nicht negativ sein
        if (waterLevel < 0) {
            waterLevel = 0;
        }

        // Akku: unter 15 % -> Wartung, bei 5 % wird der Akku getauscht
        if (batteryLevel <= 5) {
            batteryLevel = 100;
            station.setStatus(StationStatus.ONLINE);
        } else if (batteryLevel < 15) {
            station.setStatus(StationStatus.MAINTENANCE);
        }

        // höherer Wasserstand -> höhere Durchflussmenge
        double flowRate = waterLevel * 2.5;

        Measurement measurement = new Measurement(
                station.getId(),
                Instant.now(),
                round(waterLevel),
                round(flowRate),
                round(rainfall),
                round(temperature),
                batteryLevel,
                station.getStatus(),
                WarningLevel.UNKNOWN
        );

        // Warnstufe wird am Server berechnet
        measurement.setWarningLevel(stationService.calculateWarningLevel(station, measurement));

        return measurement;
    }

    private double round(double value) {
        return Math.round(value * 10) / 10.0;
    }
}