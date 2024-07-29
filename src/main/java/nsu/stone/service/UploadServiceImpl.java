package nsu.stone.service;


import nsu.stone.domain.Upload;
import nsu.stone.dto.UploadDto;
import nsu.stone.repository.UploadRepository;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UploadServiceImpl implements UploadService {

    @Autowired
    private UploadRepository uploadRepository;

    private static final double THRESHOLD = 0.0000135; // 약 1.5m에 해당하는 위도/경도 차이

    @Override
    public void processAndSaveImage(UploadDto uploadDto) {
        Upload upload = new Upload();
        upload.setImage(uploadDto.getImage());

        // OpenCV를 사용하여 이미지에서 좌표 추출
        double[] coords = getImageCoordinates(uploadDto.getImage());

        // 좌표를 지오레퍼런싱
        double[] geoCoords = georeferenceCoordinates(coords, getGeoTransform());

        // 추출한 좌표를 UploadDomain에 설정
        upload.setX1(geoCoords[0]);
        upload.setY1(geoCoords[1]);
        upload.setX2(geoCoords[2]);
        upload.setY2(geoCoords[3]);
        upload.setX3(geoCoords[4]);
        upload.setY3(geoCoords[5]);
        upload.setX4(geoCoords[6]);
        upload.setY4(geoCoords[7]);

        // 이미지 번호 설정 (겹치는 좌표 범위에 따라)
        setImageNumber(upload);

        uploadRepository.save(upload);
    }

    private double[] getImageCoordinates(byte[] imageData) {
        // 이미지 읽기
        Mat image = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
        if (image.empty()) {
            throw new IllegalArgumentException("Cannot read image");
        }

        // 그레이스케일로 변환
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);

        // 블러링
        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        // 엣지 검출
        Mat edges = new Mat();
        Imgproc.Canny(blurred, edges, 75, 200);

        // 외곽선 찾기
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        if (contours.isEmpty()) {
            throw new IllegalArgumentException("No contours found in image");
        }

        // 가장 큰 외곽선 찾기
        double maxArea = 0;
        MatOfPoint largestContour = null;
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            if (area > maxArea) {
                maxArea = area;
                largestContour = contour;
            }
        }

        if (largestContour == null) {
            throw new IllegalArgumentException("No valid contour found in image");
        }

        // 꼭짓점 찾기
        MatOfPoint2f largestContour2f = new MatOfPoint2f(largestContour.toArray());
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        Imgproc.approxPolyDP(largestContour2f, approxCurve, 0.02 * Imgproc.arcLength(largestContour2f, true), true);

        // 꼭짓점 좌표 추출
        Point[] points = approxCurve.toArray();
        double[] coords = new double[8];
        for (int i = 0; i < points.length && i < 4; i++) {
            coords[2 * i] = points[i].x;
            coords[2 * i + 1] = points[i].y;
        }

        return coords;
    }

    private double[] georeferenceCoordinates(double[] coords, double[] geoTransform) {
        double[] geoCoords = new double[8];
        geoCoords[0] = geoTransform[0] + coords[0] * geoTransform[1];
        geoCoords[1] = geoTransform[3] + coords[1] * geoTransform[5];
        geoCoords[2] = geoTransform[0] + coords[2] * geoTransform[1];
        geoCoords[3] = geoTransform[3] + coords[3] * geoTransform[5];
        geoCoords[4] = geoTransform[0] + coords[4] * geoTransform[1];
        geoCoords[5] = geoTransform[3] + coords[5] * geoTransform[5];
        geoCoords[6] = geoTransform[0] + coords[6] * geoTransform[1];
        geoCoords[7] = geoTransform[3] + coords[7] * geoTransform[5];
        return geoCoords;
    }

    private double[] getGeoTransform() {
        // 실제 프로젝트에서는 이 값을 적절히 설정해야 합니다.
        return new double[]{0, 1, 0, 0, 0, 1};
    }

    private void setImageNumber(Upload upload) {
        List<Upload> overlappingImages = uploadRepository.findOverlappingImages(
                upload.getX1(), upload.getY1(),
                upload.getX2(), upload.getY2(),
                upload.getX3(), upload.getY3(),
                upload.getX4(), upload.getY4(),
                THRESHOLD
        );

        if (overlappingImages.isEmpty()) {
            // 새로운 이미지 번호 할당
            upload.setImageNumber(getNextImageNumber());
        } else {
            // 겹치는 이미지 중 가장 작은 이미지 번호 할당
            int minImageNumber = overlappingImages.stream()
                    .mapToInt(Upload::getImageNumber)
                    .min()
                    .orElse(getNextImageNumber());
            upload.setImageNumber(minImageNumber);
        }
    }

    private int getNextImageNumber() {
        // 데이터베이스에서 현재 최대 이미지 번호를 조회하고 1을 더합니다.
        return uploadRepository.findAll().stream()
                .mapToInt(Upload::getImageNumber)
                .max()
                .orElse(0) + 1;
    }
}
