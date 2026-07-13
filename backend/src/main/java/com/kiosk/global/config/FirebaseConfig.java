package com.kiosk.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// ponytail: FIREBASE_CREDENTIALS_PATH가 없으면(팀원 로컬 개발 등) 초기화를 건너뛰고
// 경고만 남김 - 지점 로그인/가입 관련 API만 못 쓰고 나머지는 정상 기동
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() throws Exception {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS_PATH가 설정되지 않아 Firebase 초기화를 건너뜁니다. 지점 로그인/가입 API는 동작하지 않습니다.");
            return;
        }
        try (FileInputStream credentials = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }
}
