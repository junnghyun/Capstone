package nsu.stone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@Table(name = "load_db", schema = "public")
public class Map {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    @Lob
    private byte[] image;
    private String yoloData;
    private String crackType;
    private Double xCoordinate;
    private Double yCoordinate;
}

