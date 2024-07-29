package nsu.stone.config;

import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

import jakarta.servlet.MultipartConfigElement;

@Configuration
public class FileUploadConfig {
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();

        // 파일 최대 크기 설정
        factory.setMaxFileSize(DataSize.ofMegabytes(200)); // 200MB
        // 요청 최대 크기 설정
        factory.setMaxRequestSize(DataSize.ofMegabytes(200)); // 200MB

        return factory.createMultipartConfig();
    }
}
