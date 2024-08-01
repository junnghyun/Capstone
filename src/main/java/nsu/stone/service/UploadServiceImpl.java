package nsu.stone.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nsu.stone.domain.Upload;
import nsu.stone.dto.UploadDto;
import nsu.stone.repository.UploadRepository;
import org.postgis.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Service
public class UploadServiceImpl implements UploadService {

    private final UploadRepository uploadRepository;
    private final RestTemplate restTemplate;
    private static final String GEOSERVER_WPS_URL = "http://localhost:8080/geoserver/ows?service=WPS&version=1.0.0&request=Execute";

    @Autowired
    public UploadServiceImpl(UploadRepository uploadRepository, RestTemplate restTemplate) {
        this.uploadRepository = uploadRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public UploadDto processImage(String imagePath) {
        // Geoserver로부터 좌표 추출
        double[][] coordinates = getCoordinatesFromGeoserver(imagePath);

        // DB에 저장
        Upload upload = new Upload();
        upload.setImagePath(imagePath);
        upload.setTopLeft(createPostgisPoint(coordinates[0]));
        upload.setTopRight(createPostgisPoint(coordinates[1]));
        upload.setBottomLeft(createPostgisPoint(coordinates[2]));
        upload.setBottomRight(createPostgisPoint(coordinates[3]));
        upload.setStatus("COORDINATES_EXTRACTED");

        uploadRepository.save(upload);

        // UploadDto에 이미지 경로 설정
        UploadDto uploadDto = new UploadDto();
        uploadDto.setImagePath(imagePath);

        return uploadDto;
    }

    private Point createPostgisPoint(double[] coordinates) {
        double latitude = coordinates[0];
        double longitude = coordinates[1];
        return new Point(longitude, latitude); // PostGIS는 (longitude, latitude) 순서로 사용
    }

    private double[][] getCoordinatesFromGeoserver(String imagePath) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_XML);

            String requestBody = String.format(
                    "<wps:Execute version=\"1.0.0\" service=\"WPS\" " +
                            "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" " +
                            "xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
                            "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                            "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                            "xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 " +
                            "http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">" +
                            "<ows:Identifier>your-wps-process-id</ows:Identifier>" +
                            "<wps:DataInputs>" +
                            "<wps:Input>" +
                            "<ows:Identifier>imagePath</ows:Identifier>" +
                            "<wps:Data>" +
                            "<wps:LiteralData>%s</wps:LiteralData>" +
                            "</wps:Data>" +
                            "</wps:Input>" +
                            "</wps:DataInputs>" +
                            "<wps:ResponseForm>" +
                            "<wps:RawDataOutput>" +
                            "<ows:Identifier>result</ows:Identifier>" +
                            "</wps:RawDataOutput>" +
                            "</wps:ResponseForm>" +
                            "</wps:Execute>", imagePath);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(GEOSERVER_WPS_URL, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode root = mapper.readTree(response.getBody());

                // 응답의 구조에 맞게 조정 필요
                double[] topLeft = {root.path("topLeft").path("lat").asDouble(), root.path("topLeft").path("lon").asDouble()};
                double[] topRight = {root.path("topRight").path("lat").asDouble(), root.path("topRight").path("lon").asDouble()};
                double[] bottomLeft = {root.path("bottomLeft").path("lat").asDouble(), root.path("bottomLeft").path("lon").asDouble()};
                double[] bottomRight = {root.path("bottomRight").path("lat").asDouble(), root.path("bottomRight").path("lon").asDouble()};

                return new double[][]{topLeft, topRight, bottomLeft, bottomRight};
            } else {
                throw new RuntimeException("Failed to get response from Geoserver WPS");
            }
        } catch (IOException e) {
            throw new RuntimeException("Error processing Geoserver WPS response", e);
        }
    }
}
