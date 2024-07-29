package nsu.stone.repository;


import nsu.stone.domain.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UploadRepository extends JpaRepository<Upload, Long> {
    @Query("SELECT u FROM Upload u WHERE " +
            "ABS(u.x1 - :x1) <= :threshold AND ABS(u.y1 - :y1) <= :threshold AND " +
            "ABS(u.x2 - :x2) <= :threshold AND ABS(u.y2 - :y2) <= :threshold AND " +
            "ABS(u.x3 - :x3) <= :threshold AND ABS(u.y3 - :y3) <= :threshold AND " +
            "ABS(u.x4 - :x4) <= :threshold AND ABS(u.y4 - :y4) <= :threshold")
    List<Upload> findOverlappingImages(@Param("x1") double x1, @Param("y1") double y1,
                                       @Param("x2") double x2, @Param("y2") double y2,
                                       @Param("x3") double x3, @Param("y3") double y3,
                                       @Param("x4") double x4, @Param("y4") double y4,
                                       @Param("threshold") double threshold);

}
