package nsu.stone.controller;

import nsu.stone.service.GeoserverService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GeoserverController {
    private final GeoserverService geoserverService;

    public GeoserverController(GeoserverService geoserverService) {
        this.geoserverService = geoserverService;
    }

    @PostMapping("/start-geoserver")
    public String startGeoserver() {
        try {
            geoserverService.startGeoserver();
            return "Geoserver started successfully.";
        } catch (Exception e) {
            return "Error starting Geoserver: " + e.getMessage();
        }
    }

    @PostMapping("/stop-geoserver")
    public String stopGeoserver() {
        try {
            geoserverService.stopGeoserver();
            return "Geoserver stopped successfully.";
        } catch (Exception e) {
            return "Error stopping Geoserver: " + e.getMessage();
        }
    }
}
