package nsu.stone.service;

import nsu.stone.dto.UploadDto;


public interface ImageExifService {
    UploadDto processImage(String imagePath);
}
