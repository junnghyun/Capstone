package nsu.stone.service;

import nsu.stone.domain.Map;
import nsu.stone.dto.MapDto;
import nsu.stone.repository.MapRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MapServiceImpl implements MapService {

    private final MapRepository mapRepository;

    @Autowired
    public MapServiceImpl(MapRepository mapRepository) {
        this.mapRepository = mapRepository;
    }

    @Override
    public List<MapDto> getAllUploads() {
        List<Map> uploads = mapRepository.findAll();
        return uploads.stream().map(map -> new MapDto(
                map.getImagePath(),
                map.getTopLeft().getCoordinate().getX(), // X는 경도
                map.getTopLeft().getCoordinate().getY(), // Y는 위도
                map.getTopRight().getCoordinate().getX(),
                map.getTopRight().getCoordinate().getY(),
                map.getBottomLeft().getCoordinate().getX(),
                map.getBottomLeft().getCoordinate().getY(),
                map.getBottomRight().getCoordinate().getX(),
                map.getBottomRight().getCoordinate().getY()
        )).collect(Collectors.toList());
    }
}
