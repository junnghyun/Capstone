package nsu.stone.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import nsu.stone.domain.Upload;
import nsu.stone.dto.UploadDto;
import nsu.stone.repository.UploadRepository;
import org.postgis.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
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
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UploadServiceImpl {
}
//@Service
//public class UploadServiceImpl implements UploadService {
//
//    private final UploadRepository uploadRepository;
//    private final RestTemplate restTemplate;
//    private static final String GEOSERVER_WPS_URL = "http://localhost:8081/geoserver/ows?service=WPS&version=1.0.0&request=Execute";
//
//    @Autowired
//    public UploadServiceImpl(UploadRepository uploadRepository) {
//        this.uploadRepository = uploadRepository;
//        this.restTemplate = new RestTemplate();
//
//        // XML을 처리할 수 있는 HttpMessageConverter 추가
//        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
//        messageConverters.add(new StringHttpMessageConverter());
//        messageConverters.add(new ByteArrayHttpMessageConverter());
//        this.restTemplate.setMessageConverters(messageConverters);
//    }

//    @Override
//    public UploadDto processImage(String imagePath) {
//        double latitude, longitude;
//        int imageWidth, imageHeight;
//
//        try {
//            // 이미지 메타데이터 및 좌표 추출
//            File imageFile = new File(imagePath);
//            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
//            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
//            if (gpsDirectory != null) {
//                GeoLocation geoLocation = gpsDirectory.getGeoLocation();
//                if (geoLocation != null) {
//                    latitude = geoLocation.getLatitude();
//                    longitude = geoLocation.getLongitude();
//                } else {
//                    throw new RuntimeException("유효한 GPS 데이터를 찾을 수 없습니다.");
//                }
//            } else {
//                throw new RuntimeException("GPS 데이터가 없습니다.");
//            }
//
//            ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
//            if (exifDirectory != null &&
//                    exifDirectory.containsTag(ExifSubIFDDirectory.TAG_IMAGE_WIDTH) &&
//                    exifDirectory.containsTag(ExifSubIFDDirectory.TAG_IMAGE_HEIGHT)) {
//                imageWidth = exifDirectory.getInt(ExifSubIFDDirectory.TAG_IMAGE_WIDTH);
//                imageHeight = exifDirectory.getInt(ExifSubIFDDirectory.TAG_IMAGE_HEIGHT);
//            } else {
//                BufferedImage image = ImageIO.read(imageFile);
//                imageWidth = image.getWidth();
//                imageHeight = image.getHeight();
//            }
//
//            double latRange = 38.0 - 34.0;
//            double lonRange = 131.0 - 125.0;
//
//            double topLeftLat = latitude;
//            double topLeftLon = longitude;
//            double topRightLat = latitude;
//            double topRightLon = longitude + (lonRange / imageWidth);
//            double bottomLeftLat = latitude - (latRange / imageHeight);
//            double bottomLeftLon = longitude;
//            double bottomRightLat = latitude - (latRange / imageHeight);
//            double bottomRightLon = longitude + (lonRange / imageWidth);
//
//            String inputCRS = "EPSG:4326";
//            double[][] coordinates = getCoordinatesFromGeoserver(inputCRS,
//                    formatCoordinate(topLeftLat, topLeftLon),
//                    formatCoordinate(topRightLat, topRightLon),
//                    formatCoordinate(bottomLeftLat, bottomLeftLon),
//                    formatCoordinate(bottomRightLat, bottomRightLon));
//
//            Upload upload = new Upload();
//            upload.setImagePath(imagePath);
//            upload.setTopLeft(createPostgisPoint(coordinates[0]));
//            upload.setTopRight(createPostgisPoint(coordinates[1]));
//            upload.setBottomLeft(createPostgisPoint(coordinates[2]));
//            upload.setBottomRight(createPostgisPoint(coordinates[3]));
//
//            uploadRepository.save(upload);
//
//            UploadDto uploadDto = new UploadDto();
//            uploadDto.setImagePath(imagePath);
//
//            return uploadDto;
//
//        } catch (Exception e) {
//            throw new RuntimeException("이미지에서 EXIF 데이터를 추출하는 동안 오류가 발생했습니다: " + e.getMessage(), e);
//        }
//    }
//
//    private Point createPostgisPoint(double[] coordinates) {
//        double latitude = coordinates[0];
//        double longitude = coordinates[1];
//        return new Point(longitude, latitude); // PostGIS는 (longitude, latitude) 순서로 사용
//    }
//
//    private double[][] getCoordinatesFromGeoserver(String inputCRS,
//                                                   String topLeft, String topRight,
//                                                   String bottomLeft, String bottomRight) {
//        try {
//            HttpEntity<String> entity = createHttpEntity(inputCRS, topLeft, topRight, bottomLeft, bottomRight);
//            ResponseEntity<byte[]> response = restTemplate.exchange(GEOSERVER_WPS_URL, HttpMethod.POST, entity, byte[].class);
//
//            if (response.getStatusCode() == HttpStatus.OK) {
//                byte[] responseBody = response.getBody();
//                if (responseBody == null || responseBody.length == 0) {
//                    throw new RuntimeException("Response body is null or empty from GeoServer WPS");
//                }
//
//                String responseString = new String(responseBody, StandardCharsets.UTF_8);
//
//                Document doc = parseXml(responseString);
//
//                double[] topLeftCoord = extractCoordinates(doc, "topLeft");
//                double[] topRightCoord = extractCoordinates(doc, "topRight");
//                double[] bottomLeftCoord = extractCoordinates(doc, "bottomLeft");
//                double[] bottomRightCoord = extractCoordinates(doc, "bottomRight");
//
//                return new double[][]{topLeftCoord, topRightCoord, bottomLeftCoord, bottomRightCoord};
//            } else {
//                throw new RuntimeException("Failed to get response from GeoServer WPS: " + response.getStatusCode());
//            }
//        } catch (Exception e) {
//            throw new RuntimeException("Error processing GeoServer WPS response: " + e.getMessage(), e);
//        }
//    }
//
//    private Document parseXml(String xmlString) throws Exception {
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        return builder.parse(new InputSource(new StringReader(xmlString)));
//    }
//
//    private double[] extractCoordinates(Document doc, String position) {
//        NodeList coordinatesList = doc.getElementsByTagName("gml:coordinates");
//        if (coordinatesList.getLength() == 0) {
//            throw new RuntimeException("No coordinates node found.");
//        }
//
//        String[] latLon = coordinatesList.item(0).getTextContent().split(",");
//        if (latLon.length < 2) {
//            throw new RuntimeException("Not enough values for position: " + position);
//        }
//
//        double lon = Double.parseDouble(latLon[0].trim());
//        double lat = Double.parseDouble(latLon[1].trim());
//
//        return new double[]{lat, lon};
//    }
//
//    private HttpEntity<String> createHttpEntity(String inputCRS,
//                                                String topLeft, String topRight,
//                                                String bottomLeft, String bottomRight) {
//        if (inputCRS == null || inputCRS.isEmpty()) {
//            throw new IllegalArgumentException("Input CRS must not be null or empty.");
//        }
//
//        HttpHeaders headers = new HttpHeaders();
//        headers.setContentType(MediaType.APPLICATION_XML);
//        headers.setAccept(Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML));
//
//        String requestBody = String.format(
//                "<?xml version=\"1.0\" encoding=\"UTF-8\"?> " +
//                        "<wps:Execute version=\"1.0.0\" service=\"WPS\" " +
//                        "xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" " +
//                        "xmlns:ows=\"http://www.opengis.net/ows/1.1\" " +
//                        "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
//                        "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
//                        "xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 " +
//                        "http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">" +
//                        "<ows:Identifier>gs:ReprojectGeometry</ows:Identifier>" +
//                        "<wps:DataInputs>" +
//                        "<wps:Input>" +
//                        "<ows:Identifier>sourceCRS</ows:Identifier>" +
//                        "<wps:Data>" +
//                        "<wps:LiteralData>EPSG:4326</wps:LiteralData>" +
//                        "</wps:Data>" +
//                        "</wps:Input>" +
//                        "<wps:Input>" +
//                        "<ows:Identifier>targetCRS</ows:Identifier>" +
//                        "<wps:Data>" +
//                        "<wps:LiteralData>EPSG:3857</wps:LiteralData>" +
//                        "</wps:Data>" +
//                        "</wps:Input>" +
//                        "<wps:Input>" +
//                        "<ows:Identifier>geometry</ows:Identifier>" +
//                        "<wps:Data>" +
//                        "<wps:ComplexData mimeType=\"application/gml+xml\">" +
//                        "MULTIPOINT((%s),(%s),(%s),(%s))" +
//                        "</wps:ComplexData>" +
//                        "</wps:Data>" +
//                        "</wps:Input>" +
//                        "</wps:DataInputs>" +
//                        "<wps:ResponseForm>" +
//                        "<wps:RawDataOutput mimeType=\"application/gml+xml\">" +
//                        "<ows:Identifier>result</ows:Identifier>" +
//                        "</wps:RawDataOutput>" +
//                        "</wps:ResponseForm>" +
//                        "</wps:Execute>", topLeft, topRight, bottomLeft, bottomRight);
//
//        return new HttpEntity<>(requestBody, headers);
//    }
//
//    private String formatCoordinate(double latitude, double longitude) {
//        return String.format("%.6f %.6f", longitude, latitude);
//    }
//}
