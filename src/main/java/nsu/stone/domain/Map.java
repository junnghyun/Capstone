package nsu.stone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

@Entity
@Getter @Setter
@Table(name = "upload")
public class Map {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "top_left", columnDefinition = "Geometry(Point, 5181)")
    private Point topLeft;

    @Column(name = "top_right", columnDefinition = "Geometry(Point, 5181)")
    private Point topRight;

    @Column(name = "bottom_left", columnDefinition = "Geometry(Point, 5181)")
    private Point bottomLeft;

    @Column(name = "bottom_right", columnDefinition = "Geometry(Point, 5181)")
    private Point bottomRight;
}

