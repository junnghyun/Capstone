package nsu.stone.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.GpsDirectory;
import nsu.stone.domain.Upload;
import nsu.stone.dto.UploadDto;
import nsu.stone.repository.UploadRepository;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageExifServiceImpl implements ImageExifService {

    private final UploadRepository uploadRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();

    @Autowired
    public ImageExifServiceImpl(UploadRepository uploadRepository) {
        this.uploadRepository = uploadRepository;
    }

    @Override
    public UploadDto processImage(String imagePath) {
        try {
            File imageFile = new File(imagePath);
            Metadata metadata = readMetadata(imageFile);
            GeoLocation geoLocation = extractGeoLocation(metadata);
            double altitude = extractAltitude(metadata);  // 고도 추출

            if (geoLocation != null) {
                // 이미지 너비와 높이 추출
                Map<String, Integer> dimensions = getImageDimensions(imageFile);
                int width = dimensions.get("width");
                int height = dimensions.get("height");

                // 각 모서리 좌표 계산 (고도 및 시야각 반영)
                Map<String, double[]> edgeCoordinates = calculateCoordinatesAtEdges(geoLocation.getLatitude(), geoLocation.getLongitude(), altitude, width, height);

                // EPSG:5186 좌표계로 변환
                Map<String, double[]> edgeCoordinatesEPSG5186 = convertCoordinatesToEPSG5186(edgeCoordinates);

                // Geometry 객체로 변환 및 저장
                Upload upload = createAndSaveUpload(imagePath, edgeCoordinatesEPSG5186);

                // UploadDto 객체 생성 및 반환
                return createUploadDto(upload);
            } else {
                throw new RuntimeException("유효한 GPS 데이터를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException("이미지에서 EXIF 데이터를 추출하는 동안 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    private Metadata readMetadata(File imageFile) throws IOException {
        try {
            return ImageMetadataReader.readMetadata(imageFile);
        } catch (Exception e) {
            throw new IOException("메타데이터를 읽는 중 오류가 발생했습니다.", e);
        }
    }

    private GeoLocation extractGeoLocation(Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        return gpsDirectory != null ? gpsDirectory.getGeoLocation() : null;
    }

    private double extractAltitude(Metadata metadata) {
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);
        if (gpsDirectory != null && gpsDirectory.containsTag(GpsDirectory.TAG_ALTITUDE)) {
            try {
                return gpsDirectory.getDouble(GpsDirectory.TAG_ALTITUDE);
            } catch (MetadataException e) {
                throw new RuntimeException(e);
            }
        } else {
            return 0.0;  // 고도 정보가 없을 경우 0으로 처리
        }
    }

    private Map<String, Integer> getImageDimensions(File imageFile) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(imageFile);
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        Map<String, Integer> dimensions = new HashMap<>();
        dimensions.put("width", width);
        dimensions.put("height", height);
        return dimensions;
    }

    // 모서리 좌표 계산에 고도와 시야각 반영
    private Map<String, double[]> calculateCoordinatesAtEdges(double latitude, double longitude, double altitude, int width, int height) {
        // 카메라의 수평 및 수직 시야각 (예: 90도, 필요시 카메라 설정에 맞게 수정)
        double horizontalFov = 90.0;  // 수평 시야각
        double verticalFov = 60.0;    // 수직 시야각

        // 각도를 라디안으로 변환
        double hFovRad = Math.toRadians(horizontalFov / 2);
        double vFovRad = Math.toRadians(verticalFov / 2);

        // 지면과의 거리 (단순 삼각법으로 계산)
        double halfWidth = altitude * Math.tan(hFovRad);  // 지면에서 카메라 좌우로 떨어진 거리
        double halfHeight = altitude * Math.tan(vFovRad); // 지면에서 카메라 상하로 떨어진 거리

        // 경도 1도당 미터 환산계수(대략적인 값)
        double metersPerDegreeLat = 111320; // 1도 위도는 약 111.32 km
        double metersPerDegreeLon = metersPerDegreeLat * Math.cos(Math.toRadians(latitude)); // 위도에 따라 경도의 길이가 달라짐

        // 각 모서리의 위도와 경도 계산
        double dLat = halfHeight / metersPerDegreeLat;
        double dLon = halfWidth / metersPerDegreeLon;

        double[] leftTop = {latitude + dLat, longitude - dLon};
        double[] rightTop = {latitude + dLat, longitude + dLon};
        double[] leftBottom = {latitude - dLat, longitude - dLon};
        double[] rightBottom = {latitude - dLat, longitude + dLon};

        Map<String, double[]> coordinates = new HashMap<>();
        coordinates.put("left_top", leftTop);
        coordinates.put("right_top", rightTop);
        coordinates.put("left_bottom", leftBottom);
        coordinates.put("right_bottom", rightBottom);

        return coordinates;
    }

    private Map<String, double[]> convertCoordinatesToEPSG5186(Map<String, double[]> coordinates) {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem crs4326 = crsFactory.createFromName("EPSG:4326");
        CoordinateReferenceSystem crs5186 = crsFactory.createFromName("EPSG:5186");

        CoordinateTransformFactory transformFactory = new CoordinateTransformFactory();
        CoordinateTransform transform = transformFactory.createTransform(crs4326, crs5186);

        ProjCoordinate srcCoord = new ProjCoordinate();
        ProjCoordinate destCoord = new ProjCoordinate();

        Map<String, double[]> epsg5186Coordinates = new HashMap<>();

        for (Map.Entry<String, double[]> entry : coordinates.entrySet()) {
            double[] latLon = entry.getValue();
            srcCoord.x = latLon[1]; // 경도
            srcCoord.y = latLon[0]; // 위도

            transform.transform(srcCoord, destCoord);
            epsg5186Coordinates.put(entry.getKey(), new double[]{destCoord.x, destCoord.y});
        }

        return epsg5186Coordinates;
    }

    private Geometry createGeometry(double[] coordinates) {
        return geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(coordinates[0], coordinates[1]));
    }

    private Upload createAndSaveUpload(String imagePath, Map<String, double[]> edgeCoordinatesEPSG5186) {
        Upload upload = new Upload();
        upload.setImagePath(imagePath);
        upload.setTopLeft(createGeometry(edgeCoordinatesEPSG5186.get("left_top")));
        upload.setTopRight(createGeometry(edgeCoordinatesEPSG5186.get("right_top")));
        upload.setBottomLeft(createGeometry(edgeCoordinatesEPSG5186.get("left_bottom")));
        upload.setBottomRight(createGeometry(edgeCoordinatesEPSG5186.get("right_bottom")));

        uploadRepository.save(upload);
        return upload;
    }

    private UploadDto createUploadDto(Upload upload) {
        UploadDto uploadDto = new UploadDto();
        uploadDto.setImagePath(upload.getImagePath());
        return uploadDto;
    }
}
