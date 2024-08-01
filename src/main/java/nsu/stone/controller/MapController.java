package nsu.stone.controller;

import nsu.stone.dto.MapDto;
import nsu.stone.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/maps")
public class MapController {
    @Autowired
    private MapService mapService;

    @GetMapping("/{id}")
    public ResponseEntity<MapDto> getMapById(@PathVariable UUID id) {
        MapDto mapDto = mapService.getMapById(id);
        return ResponseEntity.ok(mapDto);
    }

    @GetMapping
    public ResponseEntity<List<MapDto>> getAllMaps() {
        List<MapDto> maps = mapService.getAllMaps();
        return ResponseEntity.ok(maps);
    }

    @PostMapping
    public ResponseEntity<MapDto> saveMap(@RequestBody MapDto mapDto) {
        MapDto savedMap = mapService.saveMap(mapDto);
        return ResponseEntity.ok(savedMap);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMap(@PathVariable UUID id) {
        mapService.deleteMap(id);
        return ResponseEntity.noContent().build();
    }
}

