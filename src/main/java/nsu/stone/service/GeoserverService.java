package nsu.stone.service;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class GeoserverService {

    private static final Logger logger = LoggerFactory.getLogger(GeoserverService.class);
    private Process geoserverProcess;

    public void startGeoserver() {
        try {
            // 이미 실행 중인지 확인
            if (geoserverProcess == null || !geoserverProcess.isAlive()) {
                ProcessBuilder processBuilder = new ProcessBuilder("/usr/local/Cellar/geoserver/2.5.3/bin/startup.sh");
                processBuilder.redirectErrorStream(true);
                geoserverProcess = processBuilder.start(); // 프로세스를 필드에 저장
                logger.info("Geoserver started.");

                // 프로세스가 종료될 때까지 대기
                geoserverProcess.waitFor(); // 이 줄에서 InterruptedException이 발생할 수 있습니다.
            } else {
                logger.warn("Geoserver is already running.");
            }
        } catch (IOException e) {
            logger.error("Error starting Geoserver: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            // 프로세스 대기 중 인터럽트가 발생한 경우 처리
            Thread.currentThread().interrupt(); // 현재 스레드의 인터럽트 상태 복원
            logger.error("Geoserver startup was interrupted.", e);
        }
    }

    public void stopGeoserver() {
        // 프로세스가 실행 중인지 확인
        if (geoserverProcess != null) {
            geoserverProcess.destroy(); // Geoserver 프로세스 종료
            geoserverProcess = null; // 프로세스 필드 초기화
            logger.info("Geoserver stopped.");
        } else {
            logger.warn("Geoserver is not running.");
        }
    }

    @PreDestroy
    public void cleanup() {
        stopGeoserver(); // 애플리케이션 종료 시 Geoserver 종료
    }
}
