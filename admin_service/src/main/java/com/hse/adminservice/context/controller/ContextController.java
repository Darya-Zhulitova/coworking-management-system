package com.hse.adminservice.context.controller;

import com.hse.adminservice.context.dto.AppContextResponse;
import com.hse.adminservice.context.service.ContextService;
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
