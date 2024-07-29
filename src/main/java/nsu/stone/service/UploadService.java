package nsu.stone.service;




import nsu.stone.domain.Upload;
import nsu.stone.dto.UploadDto;

import java.io.IOException;

public interface UploadService {
    void processAndSaveImage(UploadDto uploadDto);
}
