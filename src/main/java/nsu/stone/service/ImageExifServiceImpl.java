package nsu.stone.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
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
            GeoLocation geoLocation = extractGeoLocation(readMetadata(imageFile));

            if (geoLocation != null) {
                // 이미지 너비와 높이 추출
                Map<String, Integer> dimensions = getImageDimensions(imageFile);
                int width = dimensions.get("width");
                int height = dimensions.get("height");

                // 각 모서리 좌표 계산
                Map<String, double[]> edgeCoordinates = calculateCoordinatesAtEdges(geoLocation.getLatitude(), geoLocation.getLongitude(), width, height);

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

    private Map<String, Integer> getImageDimensions(File imageFile) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(imageFile);
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        Map<String, Integer> dimensions = new HashMap<>();
        dimensions.put("width", width);
        dimensions.put("height", height);
        return dimensions;
    }

    private Map<String, double[]> calculateCoordinatesAtEdges(double latitude, double longitude, int width, int height) {
        double latRange = 38.0 - 34.0; // 한국의 위도 범위
        double lonRange = 131.0 - 125.0; // 한국의 경도 범위

        double dLat = latRange / height;
        double dLon = lonRange / width;

        double[] leftTop = {latitude, longitude};
        double[] rightTop = {latitude, longitude + dLon};
        double[] leftBottom = {latitude - dLat, longitude};
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
