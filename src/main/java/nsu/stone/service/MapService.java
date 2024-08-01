package nsu.stone.service;

import nsu.stone.dto.MapDto;

import java.util.List;
import java.util.UUID;

public interface MapService {
    MapDto getMapById(UUID id);
    List<MapDto> getAllMaps();
    MapDto saveMap(MapDto mapDto);
    void deleteMap(UUID id);
}
