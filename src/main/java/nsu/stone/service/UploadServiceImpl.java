package nsu.stone.service;

import nsu.stone.domain.Upload;
import nsu.stone.dto.UploadDto;
import nsu.stone.repository.UploadRepository;
import org.postgis.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;

@Service
public class UploadServiceImpl implements UploadService {

    private final UploadRepository uploadRepository;
    private final RestTemplate restTemplate;
    private static final String GEOSERVER_WPS_URL = "http://localhost:8081/geoserver/ows?service=WPS&version=1.0.0&request=Execute";

    @Autowired
    public UploadServiceImpl(UploadRepository uploadRepository, RestTemplate restTemplate) {
        this.uploadRepository = uploadRepository;
        this.restTemplate = restTemplate;
    }

    @Override
    public UploadDto processImage(String imagePath) {
        // 이미지 파일에서 너비와 높이 읽기
        int imageWidth, imageHeight;
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            imageWidth = image.getWidth();
            imageHeight = image.getHeight();
        } catch (IOException e) {
            throw new RuntimeException("Error reading image file: " + e.getMessage(), e);
        }

        // 이미지 처리에 필요한 기본 정보 설정
        String inputCRS = "EPSG:4326";

        // 이미지의 네 모서리 좌표 설정 (픽셀 좌표계)
        String topLeft = "0 0";
        String topRight = imageWidth + " 0";
        String bottomLeft = "0 " + imageHeight;
        String bottomRight = imageWidth + " " + imageHeight;

        // Geoserver로부터 좌표 추출
        double[][] coordinates = getCoordinatesFromGeoserver(inputCRS, topLeft, topRight, bottomLeft, bottomRight);

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

    private double[][] getCoordinatesFromGeoserver(String inputCRS,
                                                   String topLeft, String topRight,
                                                   String bottomLeft, String bottomRight) {
        try {
            HttpEntity<String> entity = createHttpEntity(inputCRS, topLeft, topRight, bottomLeft, bottomRight);
            ResponseEntity<String> response = restTemplate.exchange(GEOSERVER_WPS_URL, HttpMethod.POST, entity, String.class);

            // 응답 상태 코드 확인
            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();

                // 응답 본문이 null인지 체크
                if (responseBody == null) {
                    throw new RuntimeException("Response body is null from GeoServer WPS");
                }

                // GeoServer로부터의 응답 출력
                System.out.println("Response from GeoServer: " + responseBody);

                // 예외 응답 확인
                if (responseBody.contains("ExceptionReport")) {
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(new InputSource(new StringReader(responseBody)));
                    NodeList exceptionText = doc.getElementsByTagName("ows:ExceptionText");
                    if (exceptionText.getLength() > 0) {
                        throw new RuntimeException("GeoServer WPS Exception: " + exceptionText.item(0).getTextContent());
                    }
                }

                // XML 파싱
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(responseBody)));

                // XML에서 좌표 추출 (응답 형식에 맞게 조정 필요)
                double[] topLeftCoord = extractCoordinates(doc, "topLeft");
                double[] topRightCoord = extractCoordinates(doc, "topRight");
                double[] bottomLeftCoord = extractCoordinates(doc, "bottomLeft");
                double[] bottomRightCoord = extractCoordinates(doc, "bottomRight");

                return new double[][]{topLeftCoord, topRightCoord, bottomLeftCoord, bottomRightCoord};
            } else {
                throw new RuntimeException("Failed to get response from GeoServer WPS: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing GeoServer WPS response: " + e.getMessage(), e);
        }
    }

    private double[] extractCoordinates(Document doc, String position) {
        NodeList coordinatesList = doc.getElementsByTagName("coordinates");
        if (coordinatesList.getLength() == 0) {
            throw new RuntimeException("No coordinates node found.");
        }

        NodeList positionList = coordinatesList.item(0).getChildNodes();
        for (int i = 0; i < positionList.getLength(); i++) {
            if (positionList.item(i).getNodeName().equals(position)) {
                String[] latLon = positionList.item(i).getTextContent().split(",");
                if (latLon.length < 2) {
                    throw new RuntimeException("Not enough values for position: " + position);
                }
                double lat = Double.parseDouble(latLon[0]);
                double lon = Double.parseDouble(latLon[1]);
                return new double[]{lat, lon};
            }
        }
        throw new RuntimeException("No node found for position: " + position);
    }

    private HttpEntity<String> createHttpEntity(String inputCRS,
                                                String topLeft, String topRight,
                                                String bottomLeft, String bottomRight) {
        // 입력 매개변수 유효성 검사
        if (inputCRS == null || inputCRS.isEmpty()) {
            throw new IllegalArgumentException("Input CRS must not be null or empty.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON)); // JSON 응답 요청

        // WPS 요청 본문 생성
        String requestBody = String.format(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
                        "<wps:Execute version=\"1.0.0\" service=\"WPS\" " +
                        "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" " +
                        "xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
                        "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 " +
                        "http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">" +
                        "<ows:Identifier>gs:ReprojectGeometry</ows:Identifier>" +
                        "<wps:DataInputs>" +
                        "<wps:Input>" +
                        "<ows:Identifier>sourceCRS</ows:Identifier>" +
                        "<wps:Data>" +
                        "<wps:LiteralData>%s</wps:LiteralData>" +
                        "</wps:Data>" +
                        "</wps:Input>" +
                        "<wps:Input>" +
                        "<ows:Identifier>targetCRS</ows:Identifier>" +
                        "<wps:Data>" +
                        "<wps:LiteralData>EPSG:3857</wps:LiteralData>" +
                        "</wps:Data>" +
                        "</wps:Input>" +
                        "<wps:Input>" +
                        "<ows:Identifier>geometry</ows:Identifier>" +
                        "<wps:Data>" +
                        "<wps:ComplexData mimeType=\"application/wkt\">" +
                        "MULTIPOINT((%s),(%s),(%s),(%s))" +
                        "</wps:ComplexData>" +
                        "</wps:Data>" +
                        "</wps:Input>" +
                        "</wps:DataInputs>" +
                        "<wps:ResponseForm>" +
                        "<wps:RawDataOutput mimeType=\"application/xml\">" + // XML 형식으로 응답 요청
                        "<ows:Identifier>result</ows:Identifier>" +
                        "</wps:RawDataOutput>" +
                        "</wps:ResponseForm>" +
                        "</wps:Execute>",
                inputCRS, topLeft, topRight, bottomLeft, bottomRight);



        return new HttpEntity<>(requestBody, headers);
    }
}