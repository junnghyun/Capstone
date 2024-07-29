package nsu.stone.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "upload", schema = "public")
public class Upload {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer imageNumber;
    private Double x1, y1, x2, y2, x3, y3, x4, y4;

    @Lob
    private byte[] image;
}