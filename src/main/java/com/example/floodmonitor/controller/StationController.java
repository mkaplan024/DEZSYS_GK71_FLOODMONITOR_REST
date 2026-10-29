package com.example.floodmonitor.controller;

import com.example.floodmonitor.exception.ErrorResponse;
import com.example.floodmonitor.model.Measurement;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.Statistics;
import com.example.floodmonitor.model.WarningLevel;
import com.example.floodmonitor.service.StationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping(value = "/api/v1", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
@Tag(name = "Flood Monitor", description = "Messstationen, Messwerte, Statistik und Warnungen")
public class StationController {

    private final StationService stationService;

    public StationController(StationService stationService) {
        this.stationService = stationService;
    }

    @Operation(summary = "Alle Messstationen abrufen",
            description = "Liefert alle Stationen. Optional kann nach Fluss, Aktiv-Status und aktueller Warnstufe gefiltert werden.")
    @ApiResponse(responseCode = "200", description = "Liste der Stationen")
    @ApiResponse(responseCode = "400", description = "Ungültiger Parameter, z. B. unbekannte Warnstufe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "API-Key fehlt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "API-Key ungültig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstützt (nur JSON und XML)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/stations")
    public List<Station> getStations(
            @Parameter(description = "Name des Flusses, z. B. Donau")
            @RequestParam(required = false) String river,
            @Parameter(description = "true = nur aktive Stationen, false = nur inaktive Stationen")
            @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "aktuelle Warnstufe der Station")
            @RequestParam(required = false) WarningLevel warningLevel) {
        return stationService.getStations(river, isActive, warningLevel);
    }

    @Operation(summary = "Einzelne Station abrufen")
    @ApiResponse(responseCode = "200", description = "Die Station")
    @ApiResponse(responseCode = "401", description = "API-Key fehlt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "API-Key ungültig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstützt (nur JSON und XML)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/stations/{stationId}")
    public Station getStation(
            @Parameter(description = "ID der Station", example = "ST-001")
            @PathVariable String stationId) {
        return stationService.getStation(stationId);
    }

    @Operation(summary = "Aktuellsten Messwert abrufen",
            description = "Liefert die neueste Messung der Station. Gibt es noch keine Messung, kommt 204.")
    @ApiResponse(responseCode = "200", description = "Die neueste Messung")
    @ApiResponse(responseCode = "204", description = "Station existiert, hat aber noch keine Messung", content = @Content)
    @ApiResponse(responseCode = "401", description = "API-Key fehlt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "API-Key ungültig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstützt (nur JSON und XML)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/stations/{stationId}/measurements/latest")
    public ResponseEntity<Measurement> getLatestMeasurement(
            @Parameter(description = "ID der Station", example = "ST-001")
            @PathVariable String stationId) {
        Measurement latest = stationService.getLatestMeasurement(stationId);
        if (latest == null) {
            // Station gibt es, aber noch keine Messung -> 204 No Content
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(latest);
    }

    @Operation(summary = "Historische Messwerte abrufen",
            description = "Liefert die Messwerte einer Station, neueste zuerst. "
                    + "Optional nach Zeitraum und Warnstufe filterbar.")
    @ApiResponse(responseCode = "200", description = "Liste der Messwerte")
    @ApiResponse(responseCode = "400", description = "Ungültiger Parameter: from nach to, falsches Datumsformat, "
            + "unbekannte Warnstufe oder limit nicht zwischen 1 und 1000",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "API-Key fehlt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "API-Key ungültig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstützt (nur JSON und XML)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/stations/{stationId}/measurements")
    public List<Measurement> getMeasurements(
            @Parameter(description = "ID der Station", example = "ST-001")
            @PathVariable String stationId,
            @Parameter(description = "Beginn des Zeitraums im ISO-8601-Format, z. B. 2026-05-01T08:00:00Z")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "Ende des Zeitraums im ISO-8601-Format, z. B. 2026-05-01T12:00:00Z")
            @RequestParam(required = false) Instant to,
            @Parameter(description = "maximale Anzahl der Messwerte (1 bis 1000)")
            @RequestParam(defaultValue = "100") int limit,
            @Parameter(description = "nur Messwerte mit dieser Warnstufe")
            @RequestParam(required = false) WarningLevel warningLevel) {
        return stationService.getMeasurements(stationId, from, to, limit, warningLevel);
    }

    @Operation(summary = "Statistik einer Station abrufen",
            description = "Niedrigster, höchster und durchschnittlicher Wasserstand, durchschnittliche Durchflussmenge, "
                    + "Summe des Niederschlags, Anzahl der Messungen, Warnungen und kritischen Warnungen.")
    @ApiResponse(responseCode = "200", description = "Die Statistik")
    @ApiResponse(responseCode = "400", description = "Ungültiger Zeitraum oder falsches Datumsformat",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "API-Key fehlt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "API-Key ungültig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Station nicht gefunden",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstützt (nur JSON und XML)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/stations/{stationId}/statistics")
    public Statistics getStatistics(
            @Parameter(description = "ID der Station", example = "ST-001")
            @PathVariable String stationId,
            @Parameter(description = "Beginn des Zeitraums im ISO-8601-Format")
            @RequestParam(required = false) Instant from,
            @Parameter(description = "Ende des Zeitraums im ISO-8601-Format")
            @RequestParam(required = false) Instant to) {
        return stationService.getStatistics(stationId, from, to);
    }

    @Operation(summary = "Aktuelle Warnungen abrufen",
            description = "Liefert die neueste Messung aller Stationen, deren aktuelle Warnstufe WARNING oder CRITICAL ist.")
    @ApiResponse(responseCode = "200", description = "Liste der aktuellen Warnungen")
    @ApiResponse(responseCode = "400", description = "Ungültige Warnstufe",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "API-Key fehlt",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "API-Key ungültig",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "406", description = "Format nicht unterstützt (nur JSON und XML)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/alerts")
    public List<Measurement> getAlerts(
            @Parameter(description = "nur Warnungen mit dieser Warnstufe (WARNING oder CRITICAL)")
            @RequestParam(required = false) WarningLevel warningLevel,
            @Parameter(description = "Name des Flusses, z. B. Donau")
            @RequestParam(required = false) String river) {
        return stationService.getAlerts(warningLevel, river);
    }
}