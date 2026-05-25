package com.hse.adminservice.testinfra;

import com.hse.adminservice.files.FileStorageService;
import com.hse.adminservice.files.FileStorageProperties;
import com.hse.adminservice.images.ImageStorageService;
import com.hse.adminservice.images.ImageFileKeys;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;

@TestConfiguration
public class AdminServiceTestConfiguration {
    @Bean
    @Primary
    public FakeUserOperationsClient fakeUserOperationsClient() {
        return new FakeUserOperationsClient();
    }

    @Bean
    @Primary
    public FakeUserBookingImpactPort fakeUserBookingImpactPort() {
        return new FakeUserBookingImpactPort();
    }

    @Bean
    @Primary
    public FileStorageService fileStorageService() {
        return new FileStorageService(null, new FileStorageProperties("http://localhost", "access", "secret", "test", "us-east-1", 15)) {
            @Override
            public String presignedUrl(String fileId) {
                return fileId == null ? null : "https://files.test/" + fileId;
            }
        };
    }

    @Bean
    @Primary
    public ImageStorageService imageStorageService() {
        return new ImageStorageService(null, null, null) {
            @Override
            public ImageFileKeys uploadFloorPlan(Long floorId, MultipartFile file) {
                return new ImageFileKeys(
                        "images/floors/%d/test/full.jpg".formatted(floorId),
                        null
                );
            }

            @Override
            public ImageFileKeys uploadPlacePhoto(Long placeId, MultipartFile file) {
                return new ImageFileKeys(
                        "images/places/%d/test/full.jpg".formatted(placeId),
                        "images/places/%d/test/preview.jpg".formatted(placeId)
                );
            }

            @Override
            public ImageFileKeys uploadCoworkingPhoto(Long coworkingId, MultipartFile file) {
                return new ImageFileKeys(
                        "images/coworkings/%d/test/full.jpg".formatted(coworkingId),
                        null
                );
            }
        };
    }
}
