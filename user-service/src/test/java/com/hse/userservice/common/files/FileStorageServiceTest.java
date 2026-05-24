package com.hse.userservice.common.files;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {
    @Mock S3Client s3Client;
    @Mock S3Presigner s3Presigner;

    private FileStorageService service;

    @BeforeEach
    void setUp() {
        service = new FileStorageService(
                s3Client,
                s3Presigner,
                new FileStorageProperties(
                        "https://s3.example.test",
                        "access",
                        "secret",
                        "bucket-name",
                        "ru-1",
                        15
                )
        );
    }

    @Test
    void uploadServiceRequestAttachmentRejectsNullEmptyAndTooLargeFiles() {
        assertThatThrownBy(() -> service.uploadServiceRequestAttachment(10L, 20L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("пустым");

        MockMultipartFile empty = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        assertThatThrownBy(() -> service.uploadServiceRequestAttachment(10L, 20L, empty))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("пустым");

        MultipartFile huge = mock(MultipartFile.class);
        when(huge.isEmpty()).thenReturn(false);
        when(huge.getSize()).thenReturn(50L * 1024 * 1024 + 1);
        assertThatThrownBy(() -> service.uploadServiceRequestAttachment(10L, 20L, huge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("50 МБ");

        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void uploadServiceRequestAttachmentStoresFileInServiceRequestFolderAndReturnsPresignedUrl() throws Exception {
        stubPresignedUrl("https://files.example.test/object");
        MockMultipartFile file = new MockMultipartFile("file", "invoice.PDF", "application/pdf", "content".getBytes());

        StoredFileResponse response = service.uploadServiceRequestAttachment(10L, 20L, file);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("bucket-name");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(requestCaptor.getValue().key()).startsWith("service-requests/10/messages/20/").endsWith(".pdf");
        assertThat(response.fileId()).isEqualTo(requestCaptor.getValue().key());
        assertThat(response.url()).isEqualTo("https://files.example.test/object");
    }

    @Test
    void uploadServiceRequestAttachmentSanitizesOriginalExtensionAndFallsBackToContentTypeWhenExtensionIsUnsafe() throws Exception {
        stubPresignedUrl("https://files.example.test/object");
        MockMultipartFile unsafe = new MockMultipartFile("file", "my-file.p$n#g", "image/png", "content".getBytes());

        StoredFileResponse sanitized = service.uploadServiceRequestAttachment(10L, 20L, unsafe);

        assertThat(sanitized.fileId()).endsWith(".png");
        assertThat(sanitized.fileId()).doesNotContain("$", "#");

        MockMultipartFile longExtension = new MockMultipartFile(
                "file",
                "image.veryverylongextension",
                "image/webp",
                "content".getBytes()
        );
        StoredFileResponse fallback = service.uploadServiceRequestAttachment(10L, 21L, longExtension);

        assertThat(fallback.fileId()).endsWith(".webp");
    }

    @Test
    void uploadServiceRequestAttachmentWrapsStorageErrors() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "invoice.pdf", "application/pdf", "content".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(new RuntimeException("s3 down"));

        assertThatThrownBy(() -> service.uploadServiceRequestAttachment(10L, 20L, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Не удалось загрузить вложение");
    }

    @Test
    void presignedUrlReturnsNullForBlankFileIdAndUsesConfiguredTtlForValidFileId() throws Exception {
        assertThat(service.presignedUrl(null)).isNull();
        assertThat(service.presignedUrl("  ")).isNull();

        stubPresignedUrl("https://files.example.test/object");

        String url = service.presignedUrl("file-1");

        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(url).isEqualTo("https://files.example.test/object");
        assertThat(captor.getValue().signatureDuration()).isEqualTo(java.time.Duration.ofMinutes(15));
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("bucket-name");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("file-1");
    }

    @Test
    void fileStoragePropertiesUseOneHourDefaultTtlWhenConfiguredValueIsMissingOrInvalid() {
        assertThat(new FileStorageProperties("e", "a", "s", "b", "r", null).effectivePresignedUrlTtlMinutes()).isEqualTo(60);
        assertThat(new FileStorageProperties("e", "a", "s", "b", "r", 0).effectivePresignedUrlTtlMinutes()).isEqualTo(60);
        assertThat(new FileStorageProperties("e", "a", "s", "b", "r", -1).effectivePresignedUrlTtlMinutes()).isEqualTo(60);
        assertThat(new FileStorageProperties("e", "a", "s", "b", "r", 10).effectivePresignedUrlTtlMinutes()).isEqualTo(10);
    }

    private void stubPresignedUrl(String url) throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(new URL(url));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);
    }
}
