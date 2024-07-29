package nsu.stone.dto;


import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter @Setter
public class UploadDto {
    private Long id;
    private byte[] image;
}
