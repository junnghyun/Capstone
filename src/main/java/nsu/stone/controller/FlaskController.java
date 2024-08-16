package nsu.stone.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class FlaskController {

    @GetMapping("/call-falsk")
    public String callFlaskService() {
        RestTemplate restTemplate = new RestTemplate();
        String flaskUrl = "http://lacalhost:5000/falsk-endpoint";
        return restTemplate.getForObject(flaskUrl, String.class);
    }
}
