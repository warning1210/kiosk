package com.kiosk.hq.upload.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class HqUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다.");
        }

        String originalName = file.getOriginalFilename();

        String extension = getExtension(originalName);

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "PNG, JPG, JPEG 파일만 업로드할 수 있습니다."
            );
        }

        try {
            Path uploadPath = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            // UUID 기반 신규 파일명 생성
            String storedFilename = UUID.randomUUID()
                    + "."
                    + extension;

            Path target = uploadPath.resolve(storedFilename)
                    .normalize();

            // 최종 경로 검증 (추가 방어)
            if (!target.startsWith(uploadPath)) {
                throw new SecurityException("잘못된 파일 경로입니다.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(
                        inputStream,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return "/uploads/" + storedFilename;

        } catch (IOException e) {
            throw new RuntimeException(
                    "이미지 업로드에 실패했습니다.",
                    e
            );
        }
    }


    private String getExtension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }

        int dotIndex = filename.lastIndexOf('.');

        if (dotIndex == -1 || dotIndex == filename.length() - 1) {
            return "";
        }

        return filename.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);
    }
}