package nsu.stone.service;

import nsu.stone.domain.Map;
import nsu.stone.dto.MapDto;
import nsu.stone.repository.MapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MapServiceImpl implements MapService {
    @Autowired
    private MapRepository mapRepository;

    @Override
    public MapDto getMapById(UUID id) {
        Map map = mapRepository.findById(id).orElseThrow(() -> new RuntimeException("Map not found"));
        return convertToDto(map);
    }

    @Override
    public List<MapDto> getAllMaps() {
        return mapRepository.findAll().stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public MapDto saveMap(MapDto mapDto) {
        Map map = convertToEntity(mapDto);
        map = mapRepository.save(map);
        return convertToDto(map);
    }

    private MapDto convertToDto(Map map) {
        MapDto mapDto = new MapDto();
        mapDto.setId(map.getId());
        mapDto.setImage(map.getImage());
        mapDto.setYoloData(map.getYoloData());
        mapDto.setCrackType(map.getCrackType());
        mapDto.setXCoordinate(map.getXCoordinate());
        mapDto.setYCoordinate(map.getYCoordinate());
        return mapDto;
    }

    private Map convertToEntity(MapDto mapDto) {
        Map map = new Map();
        map.setId(mapDto.getId());
        map.setImage(mapDto.getImage());
        map.setYoloData(mapDto.getYoloData());
        map.setCrackType(mapDto.getCrackType());
        map.setXCoordinate(mapDto.getXCoordinate());
        map.setYCoordinate(mapDto.getYCoordinate());
        return map;
    }
}
