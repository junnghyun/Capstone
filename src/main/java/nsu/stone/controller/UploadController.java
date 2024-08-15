package nsu.stone.controller;

import nsu.stone.dto.UploadDto;
import nsu.stone.service.ImageExifService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    private final ImageExifService imageExifService; // 서비스 타입 변경

    @Autowired
    public UploadController(ImageExifService imageExifService) { // 생성자에서 서비스 이름 변경
        this.imageExifService = imageExifService;
    }

    @PostMapping("/upload")
    public UploadDto uploadImage(@RequestParam("image") MultipartFile image) {
        String imagePath = saveImageToFileSystem(image);
        return imageExifService.processImage(imagePath); // 서비스 호출 변경
    }

    private String saveImageToFileSystem(MultipartFile image) {
        try {
            String directory = "/Users/anjeonghyeon/Downloads/capstone"; // 실제 저장할 경로로 변경
            String filename = image.getOriginalFilename();
            Path filepath = Paths.get(directory, filename);

            // 디렉토리 생성 여부 확인
            if (!Files.exists(Paths.get(directory))) {
                Files.createDirectories(Paths.get(directory));
            }

            Files.write(filepath, image.getBytes()); // 파일 저장
            logger.info("Image saved at: {}", filepath); // 저장된 경로 출력
            return filepath.toString();
        } catch (IOException e) {
            logger.error("Failed to save image", e); // 예외 발생 시 로깅
            throw new RuntimeException("Failed to save image", e);
        }
    }
}
