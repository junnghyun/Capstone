package nsu.stone.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
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
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageExifServiceImpl implements ImageExifService {

    private final UploadRepository uploadRepository;
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private static final MathContext MATH_CONTEXT = new MathContext(8); // 소수점 이하 8자리

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
            double altitude = extractAltitude(metadata);
            int orientation = extractOrientation(metadata);

            if (geoLocation != null) {
                Map<String, Integer> dimensions = getImageDimensions(imageFile);
                int width = dimensions.get("width");
                int height = dimensions.get("height");

                // 각 모서리 좌표 계산 (정밀도 높임)
                Map<String, double[]> edgeCoordinates = calculateCoordinatesAtEdges(geoLocation.getLatitude(), geoLocation.getLongitude(), altitude, width, height, orientation);

                // EPSG:5186 좌표계로 변환
                Map<String, double[]> edgeCoordinatesEPSG5186 = convertCoordinatesToEPSG5186(edgeCoordinates);

                // Geometry 객체로 변환 및 저장
                Upload upload = createAndSaveUpload(imagePath, edgeCoordinatesEPSG5186);

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
            return 0.0;
        }
    }

    private int extractOrientation(Metadata metadata) {
        ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
            try {
                return directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
            } catch (MetadataException e) {
                throw new RuntimeException(e);
            }
        }
        return 1;
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

    private Map<String, double[]> calculateCoordinatesAtEdges(double latitude, double longitude, double altitude, int width, int height, int orientation) {
        double horizontalFov = 30.0;
        double verticalFov = 20.0;

        double hFovRad = Math.toRadians(horizontalFov / 2);
        double vFovRad = Math.toRadians(verticalFov / 2);

        double halfWidthMeters = altitude * Math.tan(hFovRad);
        double halfHeightMeters = altitude * Math.tan(vFovRad);

        double metersPerPixelWidth = (2 * halfWidthMeters) / (double) width;
        double metersPerPixelHeight = (2 * halfHeightMeters) / (double) height;

        double metersPerDegreeLat = 111320;
        double metersPerDegreeLon = metersPerDegreeLat * Math.cos(Math.toRadians(latitude));

        double dLatPerPixel = metersPerPixelHeight / metersPerDegreeLat;
        double dLonPerPixel = metersPerPixelWidth / metersPerDegreeLon;

        double widthHalf = width / 2.0;
        double heightHalf = height / 2.0;

        double[] leftTop = {latitude + (heightHalf * dLatPerPixel), longitude - (widthHalf * dLonPerPixel)};
        double[] rightTop = {latitude + (heightHalf * dLatPerPixel), longitude + (widthHalf * dLonPerPixel)};
        double[] leftBottom = {latitude - (heightHalf * dLatPerPixel), longitude - (widthHalf * dLonPerPixel)};
        double[] rightBottom = {latitude - (heightHalf * dLatPerPixel), longitude + (widthHalf * dLonPerPixel)};

        switch (orientation) {
            case 1:
                break;
            case 6:
                return rotateCoordinates(leftTop, rightTop, leftBottom, rightBottom, 90);
            case 3:
                return rotateCoordinates(leftTop, rightTop, leftBottom, rightBottom, 180);
            case 8:
                return rotateCoordinates(leftTop, rightTop, leftBottom, rightBottom, 270);
            default:
                throw new RuntimeException("지원하지 않는 방향 정보입니다.");
        }

        Map<String, double[]> coordinates = new HashMap<>();
        coordinates.put("left_top", leftTop);
        coordinates.put("right_top", rightTop);
        coordinates.put("left_bottom", leftBottom);
        coordinates.put("right_bottom", rightBottom);

        return coordinates;
    }

    private Map<String, double[]> rotateCoordinates(double[] leftTop, double[] rightTop, double[] leftBottom, double[] rightBottom, int angle) {
        double[] center = {(leftTop[0] + rightBottom[0]) / 2, (leftTop[1] + rightBottom[1]) / 2};
        Map<String, double[]> rotatedCoordinates = new HashMap<>();

        rotatedCoordinates.put("left_top", rotatePoint(leftTop, center, angle));
        rotatedCoordinates.put("right_top", rotatePoint(rightTop, center, angle));
        rotatedCoordinates.put("left_bottom", rotatePoint(leftBottom, center, angle));
        rotatedCoordinates.put("right_bottom", rotatePoint(rightBottom, center, angle));

        return rotatedCoordinates;
    }

    private double[] rotatePoint(double[] point, double[] center, int angle) {
        double radians = Math.toRadians(angle);
        double cosTheta = Math.cos(radians);
        double sinTheta = Math.sin(radians);

        double x = point[0] - center[0];
        double y = point[1] - center[1];

        double xNew = x * cosTheta - y * sinTheta + center[0];
        double yNew = x * sinTheta + y * cosTheta + center[1];

        return new double[]{xNew, yNew};
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
            srcCoord.x = new BigDecimal(Double.toString(latLon[1])).doubleValue(); // 경도
            srcCoord.y = new BigDecimal(Double.toString(latLon[0])).doubleValue(); // 위도

            transform.transform(srcCoord, destCoord);
            epsg5186Coordinates.put(entry.getKey(), new double[]{
                    new BigDecimal(destCoord.x).round(MATH_CONTEXT).doubleValue(),
                    new BigDecimal(destCoord.y).round(MATH_CONTEXT).doubleValue()
            });
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
