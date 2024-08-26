package nsu.stone.controller;

import nsu.stone.dto.MapDto;
import nsu.stone.service.MapService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MapController {

    private final MapService mapService;

    @Autowired
    public MapController(MapService mapService) {
        this.mapService = mapService;
    }

    // 업로드된 이미지 정보와 좌표를 JSON으로 반환하는 API
    @RequestMapping("/api/map")
    public List<MapDto> getUploads() {
        return mapService.getAllUploads();
    }
}
