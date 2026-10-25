package com.example.floodmonitor.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// sorgt vorerst dafür, dass 404 zurückkommt
// die schöne Fehlerausgabe kommt in Punkt 6
@ResponseStatus(HttpStatus.NOT_FOUND)
public class StationNotFoundException extends RuntimeException {

    public StationNotFoundException(String stationId) {
        super("Station " + stationId + " wurde nicht gefunden");
    }
}