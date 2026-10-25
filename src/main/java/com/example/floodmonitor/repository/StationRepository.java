package com.example.floodmonitor.repository;

import com.example.floodmonitor.model.Location;
import com.example.floodmonitor.model.Station;
import com.example.floodmonitor.model.StationStatus;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class StationRepository {

    private List<Station> stations = new ArrayList<>();

    public StationRepository() {
        // Wasserstände in cm: normal, Warnwert, kritischer Wert
        stations.add(new Station("ST-001", "Wien Reichsbrücke", "Donau",
                new Location(48.2290, 16.4120), 250, 450, 600, true, StationStatus.ONLINE));
        stations.add(new Station("ST-002", "Linz", "Donau",
                new Location(48.3069, 14.2858), 300, 550, 700, true, StationStatus.ONLINE));
        stations.add(new Station("ST-003", "Salzburg", "Salzach",
                new Location(47.8095, 13.0550), 200, 450, 600, true, StationStatus.ONLINE));
        stations.add(new Station("ST-004", "Graz", "Mur",
                new Location(47.0707, 15.4395), 150, 300, 400, false, StationStatus.OFFLINE));
    }

    public List<Station> findAll() {
        return stations;
    }

    public Station findById(String id) {
        for (Station station : stations) {
            if (station.getId().equals(id)) {
                return station;
            }
        }
        return null;
    }
}