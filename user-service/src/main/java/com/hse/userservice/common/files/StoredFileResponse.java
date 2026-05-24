package com.hse.userservice.common.files;


public record StoredFileResponse(
        String fileId,
        String url
) {
}
