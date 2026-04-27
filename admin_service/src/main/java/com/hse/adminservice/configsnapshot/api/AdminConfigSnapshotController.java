package com.hse.adminservice.configsnapshot.api;

import com.hse.adminservice.configsnapshot.application.CoworkingConfigSnapshotService;
import com.hse.adminservice.configsnapshot.dto.CoworkingConfigSnapshotResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/config/coworkings")
@RequiredArgsConstructor
public class AdminConfigSnapshotController {
    private final CoworkingConfigSnapshotService snapshotService;

    @GetMapping("/{coworkingId}/snapshot")
    public CoworkingConfigSnapshotResponse getSnapshot(@PathVariable Long coworkingId) {
        return snapshotService.getSnapshot(coworkingId);
    }
}
