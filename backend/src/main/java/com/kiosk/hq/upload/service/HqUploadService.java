package com.kiosk.hq.upload.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;

@Service
public class HqUploadService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    // 확장자별로 허용되는 실제 MIME 타입 (매직바이트 기준)
    private static final Map<String, Set<String>> ALLOWED_MIME_TYPES_BY_EXTENSION = Map.of(
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg")
    );

    private static final ContentInfoUtil CONTENT_INFO_UTIL = new ContentInfoUtil();

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
            byte[] content = file.getBytes();

            validateActualContentType(content, extension);

            Path uploadPath = Paths.get(uploadDir)
                    .toAbsolutePath()
                    .normalize();

            // 파일이 없으면 생성
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

            Files.write(target, content);

            return "/uploads/" + storedFilename;

        } catch (IOException e) {
            throw new RuntimeException(
                    "이미지 업로드에 실패했습니다.",
                    e
            );
        }
    }

    // 파일 확장자가 아닌 실제 바이너리 시그니처(매직바이트)로 이미지 타입을 검증한다.
    private void validateActualContentType(byte[] content, String extension) throws IOException {
        ContentInfo contentInfo = CONTENT_INFO_UTIL.findMatch(content);
        String mimeType = contentInfo != null ? contentInfo.getMimeType() : null;

        Set<String> allowedMimeTypes = ALLOWED_MIME_TYPES_BY_EXTENSION.get(extension);

        if (mimeType == null || allowedMimeTypes == null || !allowedMimeTypes.contains(mimeType)) {
            throw new IllegalArgumentException(
                    "파일 내용이 PNG, JPG, JPEG 이미지 형식이 아닙니다."
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