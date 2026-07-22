package com.kiosk.hq.branch.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

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
        // 메일 앱에서 초대 링크가 버튼으로 분명하게 보이도록 HTML 메일을 만든다.
        MimeMessage message = mailSender.createMimeMessage();
        try {
            // UTF-8 HTML 본문을 사용할 수 있도록 MIME 메시지 도우미를 구성한다.
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            // 발신 주소가 설정된 경우에만 From 값을 넣는다.
            if (from != null && !from.isBlank()) helper.setFrom(from);
            // 본점이 입력한 예비 지점장 이메일을 수신자로 지정한다.
            helper.setTo(email);
            // 받은 편지함에 표시할 제목을 지정한다.
            helper.setSubject("[배스킨라빈스] 지점 개설 신청 초대");

            // 링크가 HTML 속성이나 본문을 깨뜨리지 않도록 이스케이프한다.
            String safeInviteUrl = HtmlUtils.htmlEscape(inviteUrl);
            // 두 번째 인자를 true로 지정해 문자열을 일반 텍스트가 아닌 HTML로 렌더링한다.
            helper.setText(inviteHtml(safeInviteUrl), true);
            // 설정된 SMTP 서버를 통해 메일을 발송한다.
            mailSender.send(message);
        } catch (MessagingException exception) {
            // MIME 메시지 구성 실패를 서비스 오류로 변환해 API가 성공으로 응답하지 않게 한다.
            throw new IllegalStateException("지점 초대 메일을 만들지 못했습니다.", exception);
        }
    }

    // 주요 메일 앱에서 별도 스타일시트 없이 보이도록 인라인 스타일로 초대장을 구성한다.
    private String inviteHtml(String inviteUrl) {
        // CTA 버튼과 원문 URL을 함께 제공해 버튼을 지원하지 않는 메일 앱에서도 접근할 수 있게 한다.
        return """
                <!doctype html>
                <html lang="ko">
                  <body style="margin:0;padding:32px 16px;background:#f5f6fa;font-family:Arial,'Apple SD Gothic Neo','Noto Sans KR',sans-serif;color:#20242c;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0">
                      <tr><td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" border="0" style="max-width:600px;background:#ffffff;border:1px solid #e7e9f0;border-radius:18px;overflow:hidden;">
                          <tr><td style="height:8px;background:linear-gradient(90deg,#f15a9d,#6b66ea);"></td></tr>
                          <tr><td style="padding:38px 36px 34px;">
                            <div style="margin-bottom:14px;color:#6863e8;font-size:13px;font-weight:700;letter-spacing:.08em;">KIOSK · 본점 관리자</div>
                            <h1 style="margin:0 0 16px;font-size:26px;line-height:1.35;">지점 개설 신청 초대</h1>
                            <p style="margin:0 0 8px;color:#555d6c;font-size:15px;line-height:1.8;">본점 관리자가 지점 개설 신청서를 보내드렸습니다.</p>
                            <p style="margin:0;color:#555d6c;font-size:15px;line-height:1.8;">아래 버튼을 눌러 신청서를 작성해 주세요. 제출 후 본점 승인이 완료되면 로그인할 수 있습니다.</p>
                            <table role="presentation" cellspacing="0" cellpadding="0" border="0" style="margin:30px auto;">
                              <tr><td align="center" style="border-radius:10px;background:#6566e9;">
                                <a href="%s" target="_blank" style="display:inline-block;padding:15px 30px;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;">지점 개설 신청서 작성하기</a>
                              </td></tr>
                            </table>
                            <div style="padding:16px;background:#f7f7fc;border-radius:10px;">
                              <p style="margin:0 0 7px;color:#777f8d;font-size:12px;line-height:1.5;">버튼이 열리지 않으면 아래 주소를 브라우저에 복사해 주세요.</p>
                              <a href="%s" target="_blank" style="display:block;color:#5559d9;font-size:12px;line-height:1.6;word-break:break-all;">%s</a>
                            </div>
                            <p style="margin:22px 0 0;color:#9299a5;font-size:12px;line-height:1.6;">본인이 요청하지 않은 메일이라면 이 메일을 무시해 주세요. 초대 링크는 유효 시간이 지나면 사용할 수 없습니다.</p>
                          </td></tr>
                        </table>
                      </td></tr>
                    </table>
                  </body>
                </html>
                """.formatted(inviteUrl, inviteUrl, inviteUrl);
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
