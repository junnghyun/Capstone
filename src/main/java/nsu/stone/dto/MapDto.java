package nsu.stone.dto;

import lombok.Data;
import org.locationtech.jts.geom.Point;

@Data
public class MapDto {

    private String imagePath;
    private double topLeftLat;
    private double topLeftLng;
    private double topRightLat;
    private double topRightLng;
    private double bottomLeftLat;
    private double bottomLeftLng;
    private double bottomRightLat;
    private double bottomRightLng;

    // 생성자 및 게터/세터
    public MapDto(String imagePath, double topLeftLat, double topLeftLng,
                  double topRightLat, double topRightLng,
                  double bottomLeftLat, double bottomLeftLng,
                  double bottomRightLat, double bottomRightLng) {
        this.imagePath = imagePath;
        this.topLeftLat = topLeftLat;
        this.topLeftLng = topLeftLng;
        this.topRightLat = topRightLat;
        this.topRightLng = topRightLng;
        this.bottomLeftLat = bottomLeftLat;
        this.bottomLeftLng = bottomLeftLng;
        this.bottomRightLat = bottomRightLat;
        this.bottomRightLng = bottomRightLng;
    }
}
