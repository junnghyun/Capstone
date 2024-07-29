package nsu.stone.controller;

import nsu.stone.dto.UploadDto;
import nsu.stone.service.UploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    @Autowired
    private UploadService uploadService;

    @PostMapping
    public ResponseEntity<String> uploadImage(@RequestBody UploadDto uploadDto) {
        try {
            uploadService.processAndSaveImage(uploadDto);
            return ResponseEntity.ok("Image processed and saved successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing image: " + e.getMessage());
        }
    }
}
