package com.kiosk.hq.branch.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

// 지점 초대와 승인 결과 메일을 Spring JavaMailSender로 발송한다.
@Service
public class BranchInviteMailService {

    // 실제 SMTP 발송을 담당하는 Spring 객체다.
    private final JavaMailSender mailSender;
    // 환경변수로 실제 발송 여부를 켜고 끈다.
    private final boolean enabled;
    // 수신자에게 표시할 발신 이메일 주소다.
    private final String from;

    // 메일 발송에 필요한 설정값을 생성자로 받는다.
    public BranchInviteMailService(JavaMailSender mailSender,
            @Value("${app.mail.enabled:false}") boolean enabled,
            @Value("${app.mail.from:}") String from) {
        // Spring SMTP 발송기를 보관한다.
        this.mailSender = mailSender;
        // 실제 발송 설정을 보관한다.
        this.enabled = enabled;
        // 발신 이메일 주소를 보관한다.
        this.from = from;
    }

    // 예비 지점장에게 지점 개설 신청서 링크를 보낸다.
    public void sendInvite(String email, String inviteUrl) {
        // 메일 기능을 끈 개발 환경에서는 발송을 건너뛴다.
        if (!enabled) return;
        // 일반 텍스트 초대 메일 객체를 만든다.
        SimpleMailMessage message = new SimpleMailMessage();
        // 발신 주소가 설정된 경우에만 From 값을 넣는다.
        if (from != null && !from.isBlank()) message.setFrom(from);
        // 본점이 입력한 예비 지점장 이메일을 수신자로 지정한다.
        message.setTo(email);
        // 받은 편지함에 표시할 제목을 지정한다.
        message.setSubject("[배스킨라빈스] 지점 개설 신청 안내");
        // 일회용 URL과 승인 절차를 본문으로 안내한다.
        message.setText("아래 링크에서 지점 개설 신청서를 작성해 주세요.\n제출 후 본점 승인이 완료되면 로그인할 수 있습니다.\n\n" + inviteUrl);
        // 설정된 SMTP 서버를 통해 메일을 발송한다.
        mailSender.send(message);
    }

    // 신청 승인 또는 반려 결과를 지점장에게 알린다.
    public void sendResult(String email, boolean approved, String rejectionReason) {
        // 메일 기능을 끈 환경에서는 결과 메일을 건너뛴다.
        if (!enabled) return;
        // 결과 안내 메일 객체를 만든다.
        SimpleMailMessage message = new SimpleMailMessage();
        // 발신 주소가 설정된 경우에만 From 값을 넣는다.
        if (from != null && !from.isBlank()) message.setFrom(from);
        // 신청에 사용한 이메일을 수신자로 지정한다.
        message.setTo(email);
        // 승인 여부에 맞는 메일 제목을 지정한다.
        message.setSubject(approved ? "[배스킨라빈스] 지점 개설 신청이 승인되었습니다" : "[배스킨라빈스] 지점 개설 신청 결과 안내");
        // 승인되면 로그인 가능함을, 반려되면 본점 문의가 필요함을 안내한다.
        message.setText(approved
                // 승인 메일에는 기존 아이디로 로그인할 수 있음을 안내한다.
                ? "본점 승인이 완료되었습니다. 신청서에서 설정한 아이디와 비밀번호로 로그인해 주세요."
                // 반려 메일에는 본점 관리자가 입력한 사유를 함께 전달한다.
                : "지점 개설 신청이 반려되었습니다.\n\n반려 사유: " + rejectionReason);
        // SMTP 서버를 통해 결과 메일을 발송한다.
        mailSender.send(message);
    }
}
