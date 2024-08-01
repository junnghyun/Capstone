package nsu.stone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.postgis.Point;

@Entity
@Getter @Setter
@Table(name = "upload", schema = "public")
public class Upload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "image_path", nullable = false)
    private String imagePath;

    @Column(name = "top_left", columnDefinition = "geometry(Point, 4326)")
    private Point topLeft;

    @Column(name = "top_right", columnDefinition = "geometry(Point, 4326)")
    private Point topRight;

    @Column(name = "bottom_left", columnDefinition = "geometry(Point, 4326)")
    private Point bottomLeft;

    @Column(name = "bottom_right", columnDefinition = "geometry(Point, 4326)")
    private Point bottomRight;

    @Column(name = "status")
    private String status;

}