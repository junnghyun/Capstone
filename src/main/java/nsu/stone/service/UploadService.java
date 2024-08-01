package nsu.stone.service;

import nsu.stone.dto.UploadDto;

public interface UploadService {
    UploadDto processImage(String imagePath);
}
