package nsu.stone;

import nsu.stone.service.GeoserverService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class StoneApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoneApplication.class, args);
    }

    @Bean
    public CommandLineRunner run(GeoserverService geoserverService) {
        return args -> geoserverService.startGeoserver();
    }
}
