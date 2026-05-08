package com.hse.adminservice.admincontext.api;

import com.hse.adminservice.admincontext.application.ContextService;
import com.hse.adminservice.admincontext.dto.AppContextResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/context")
@RequiredArgsConstructor
public class ContextController {
    private final ContextService contextService;

    @GetMapping
    public AppContextResponse getContext(@RequestParam(name = "coworkingId", required = false) Long coworkingId) {
        return contextService.getContext(coworkingId);
    }
}
