package nsu.stone.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import nsu.stone.dto.UploadDto;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@Service
public class ImageExifService {

    public Map<String, Object> getExifDataFromImage(UploadDto uploadDto) {
        try {
            File imageFile = new File(uploadDto.getImagePath());
            Metadata metadata = ImageMetadataReader.readMetadata(imageFile);

            GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);

            if (gpsDirectory != null) {
                double[] latLong = gpsDirectory.getGeoLocation().getLatitudeLongitude();

                double latitude = latLong[0];
                double longitude = latLong[1];

                int width = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifSubIFDDirectory.class)
                        .getInt(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_IMAGE_WIDTH);
                int height = metadata.getFirstDirectoryOfType(com.drew.metadata.exif.ExifSubIFDDirectory.class)
                        .getInt(com.drew.metadata.exif.ExifSubIFDDirectory.TAG_IMAGE_HEIGHT);

                Map<String, double[]> edgeCoordinates = calculateCoordinatesAtEdges(latitude, longitude, width, height);

                Map<String, Object> response = new HashMap<>();
                response.put("Latitude", latitude);
                response.put("Longitude", longitude);
                response.put("TopLeftCorner", edgeCoordinates.get("left_top"));
                response.put("TopRightCorner", edgeCoordinates.get("right_top"));
                response.put("BottomLeftCorner", edgeCoordinates.get("left_bottom"));
                response.put("BottomRightCorner", edgeCoordinates.get("right_bottom"));

                return response;
            } else {
                throw new RuntimeException("GPS 데이터가 없습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException("이미지에서 EXIF 데이터를 추출하는 동안 오류가 발생했습니다.", e);
        }
    }

    private Map<String, double[]> calculateCoordinatesAtEdges(double latitude, double longitude, int width, int height) {
        double latRange = 38.0 - 34.0;  // 한국의 위도 범위
        double lonRange = 131.0 - 125.0;  // 한국의 경도 범위

        double[] leftTop = {latitude, longitude};
        double[] rightTop = {latitude, longitude + (lonRange / width)};
        double[] leftBottom = {latitude - (latRange / height), longitude};
        double[] rightBottom = {latitude - (latRange / height), longitude + (lonRange / width)};

        Map<String, double[]> coordinates = new HashMap<>();
        coordinates.put("left_top", leftTop);
        coordinates.put("right_top", rightTop);
        coordinates.put("left_bottom", leftBottom);
        coordinates.put("right_bottom", rightBottom);

        return coordinates;
    }
}
