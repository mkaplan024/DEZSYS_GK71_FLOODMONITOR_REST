package com.example.floodmonitor;

import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.repository.MeasurementRepository;
import com.example.floodmonitor.service.StationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

// Simulation ist ausgeschaltet, damit die Tests immer gleich ablaufen
@SpringBootTest(properties = "simulation.enabled=false")
@AutoConfigureMockMvc
public class FloodMonitorApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeasurementRepository measurementRepository;

    @Autowired
    private StationService stationService;

    @BeforeEach
    public void setUp() {
        // vor jedem Test alle Messungen löschen
        measurementRepository.deleteAll();
    }

    // speichert eine Messung für ST-001 mit fester Zeit und festem Wasserstand
    private void saveMeasurement(String time, double waterLevel) {
        Measurement measurement = new Measurement("ST-001", Instant.parse(time), waterLevel, waterLevel * 2.5,
                0, 12, 80, StationStatus.ONLINE, WarningLevel.NORMAL);
        measurementRepository.save(measurement);
    }

    // erstellt eine Messung nur mit Wasserstand (für die Warnstufen-Tests)
    private Measurement measurementWithWaterLevel(double waterLevel) {
        Measurement measurement = new Measurement();
        measurement.setWaterLevel(waterLevel);
        return measurement;
    }

    // 1. Abruf aller Stationen als JSON
    @Test
    public void getAllStationsAsJson() throws Exception {
        mockMvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].id").value("ST-001"));
    }

    // 2. Abruf aller Stationen als XML
    @Test
    public void getAllStationsAsXml() throws Exception {
        mockMvc.perform(get("/api/v1/stations").accept(MediaType.APPLICATION_XML))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_XML))
                .andExpect(xpath("/List/item[1]/id").string("ST-001"));
    }

    // 3. Abruf einer existierenden Station
    @Test
    public void getExistingStation() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Wien Reichsbrücke"))
                .andExpect(jsonPath("$.river").value("Donau"));
    }

    // 4. Abruf einer unbekannten Station
    @Test
    public void getUnknownStation() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Station ST-999 wurde nicht gefunden"));
    }

    // 5. Abruf des aktuellsten Messwerts
    @Test
    public void getLatestMeasurement() throws Exception {
        saveMeasurement("2026-05-01T08:00:00Z", 300);
        saveMeasurement("2026-05-01T09:00:00Z", 320);

        mockMvc.perform(get("/api/v1/stations/ST-001/measurements/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waterLevel").value(320.0))
                .andExpect(jsonPath("$.timestamp").value("2026-05-01T09:00:00Z"));
    }

    // 6. Filterung nach Zeitraum
    @Test
    public void filterByTimeRange() throws Exception {
        saveMeasurement("2026-05-01T08:00:00Z", 300);
        saveMeasurement("2026-05-01T10:00:00Z", 310);
        saveMeasurement("2026-05-01T12:00:00Z", 320);

        // nur 10:00 und 12:00 liegen im Zeitraum
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements")
                        .param("from", "2026-05-01T09:00:00Z")
                        .param("to", "2026-05-01T13:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].timestamp").value("2026-05-01T12:00:00Z"))
                .andExpect(jsonPath("$[1].timestamp").value("2026-05-01T10:00:00Z"));
    }

    // 7. Ungültiger Zeitraum (from nach to)
    @Test
    public void invalidTimeRange() throws Exception {
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements")
                        .param("from", "2026-05-01T12:00:00Z")
                        .param("to", "2026-05-01T08:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // 8. Warnstufe NORMAL (ST-001: Warnwert 450 cm, kritisch 600 cm)
    @Test
    public void warningLevelNormal() {
        Station station = stationService.getStation("ST-001");
        assertEquals(WarningLevel.NORMAL, stationService.calculateWarningLevel(station, measurementWithWaterLevel(300)));
    }

    // 9. Warnstufe WARNING
    @Test
    public void warningLevelWarning() {
        Station station = stationService.getStation("ST-001");
        assertEquals(WarningLevel.WARNING, stationService.calculateWarningLevel(station, measurementWithWaterLevel(450)));
        assertEquals(WarningLevel.WARNING, stationService.calculateWarningLevel(station, measurementWithWaterLevel(500)));
    }

    // 10. Warnstufe CRITICAL
    @Test
    public void warningLevelCritical() {
        Station station = stationService.getStation("ST-001");
        assertEquals(WarningLevel.CRITICAL, stationService.calculateWarningLevel(station, measurementWithWaterLevel(600)));
        assertEquals(WarningLevel.CRITICAL, stationService.calculateWarningLevel(station, measurementWithWaterLevel(700)));
    }

    // 11. Nicht unterstütztes Format
    @Test
    public void unsupportedFormat() throws Exception {
        mockMvc.perform(get("/api/v1/stations").accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isNotAcceptable());
    }

    // 12. Maximales Limit
    @Test
    public void maxLimit() throws Exception {
        // 1000 ist genau das Maximum -> erlaubt
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "1000"))
                .andExpect(status().isOk());

        // 1001 ist zu viel -> Fehler
        mockMvc.perform(get("/api/v1/stations/ST-001/measurements").param("limit", "1001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("limit darf maximal 1000 sein"));
    }
}