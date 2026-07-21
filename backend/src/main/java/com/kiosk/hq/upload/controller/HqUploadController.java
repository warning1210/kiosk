package com.kiosk.hq.upload.controller;

import com.kiosk.global.security.HqAccessService;
import com.kiosk.hq.upload.dto.HqUploadResponse;
import com.kiosk.hq.upload.service.HqUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/hq/uploads")
@RequiredArgsConstructor
public class HqUploadController {

    private final HqUploadService hqUploadService;
    private final HqAccessService hqAccessService;

    @PostMapping
    public HqUploadResponse upload(@RequestParam("file") MultipartFile file,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        hqAccessService.requireAdmin(authorization);
        return new HqUploadResponse(hqUploadService.store(file));
    }
}
