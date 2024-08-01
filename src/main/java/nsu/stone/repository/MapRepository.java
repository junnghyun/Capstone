package nsu.stone.repository;

import nsu.stone.domain.Map;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface MapRepository extends JpaRepository<Map, UUID> {
}
