package nsu.stone.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class MapDto {
    private UUID id;
    private byte[] image;
    private String yoloData;
    private String crackType;
    private Double xCoordinate;
    private Double yCoordinate;
}
