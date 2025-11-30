package com.healthlink.infrastructure.email;

import com.healthlink.domain.notification.entity.EmailDispatch;
import com.healthlink.domain.notification.repository.EmailDispatchRepository;
import com.healthlink.domain.user.entity.User;
import com.healthlink.domain.user.enums.ApprovalStatus;
import com.healthlink.domain.user.repository.UserRepository;
import com.healthlink.infrastructure.logging.SafeLogger;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import java.io.UnsupportedEncodingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Sends approval / rejection decision emails for accounts requiring manual review.
 * Persists each attempt in EmailDispatch for auditing & potential retries.
 */
@Service
@RequiredArgsConstructor
public class ApprovalEmailService {

    private final JavaMailSender mailSender;
    private final EmailDispatchRepository dispatchRepository;
    private final UserRepository userRepository;
    private final SafeLogger log = SafeLogger.get(ApprovalEmailService.class);

    @Transactional
    public void sendApprovalDecision(UUID userId, ApprovalStatus newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found for approval email"));

        // Only send for terminal decisions
        if (newStatus != ApprovalStatus.APPROVED && newStatus != ApprovalStatus.REJECTED) {
            return; // ignore intermediary states
        }

        EmailDispatch dispatch = new EmailDispatch();
        dispatch.setUserId(userId);
        dispatch.setEmailType("APPROVAL_DECISION");
        dispatch.setAttemptedAt(OffsetDateTime.now());

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());
            try {
                helper.setTo(new InternetAddress(user.getEmail(), user.getFullName()));
            } catch (UnsupportedEncodingException uee) {
                helper.setTo(user.getEmail()); // Fallback without personal name
            }
            helper.setSubject(buildSubject(newStatus));
            helper.setText(buildHtmlBody(user, newStatus), true);
            // From address (can be overridden by env var) falls back to noreply
            helper.setFrom("noreply@healthlink.local");

            mailSender.send(mimeMessage);
            dispatch.setStatus("SENT");
            log.event("approval_email_sent")
               .with("userId", userId.toString())
               .with("status", newStatus.name())
               .log();
        } catch (MessagingException | RuntimeException ex) {
            dispatch.setStatus("FAILED");
            dispatch.setErrorMessage(ex.getMessage());
            log.event("approval_email_failed")
               .with("userId", userId.toString())
               .with("status", newStatus.name())
               .with("error", ex.getClass().getSimpleName())
               .log();
        }

        dispatchRepository.save(dispatch);
    }

    private String buildSubject(ApprovalStatus status) {
        return switch (status) {
            case APPROVED -> "Your HealthLink account has been approved";
            case REJECTED -> "HealthLink account application decision";
            default -> "HealthLink account update";
        };
    }

    private String buildHtmlBody(User user, ApprovalStatus status) {
        String decisionBlock = status == ApprovalStatus.APPROVED ? approvedBlock() : rejectedBlock();
        return "" +
                "<html><body style='font-family:Arial,sans-serif;background:#f9f9f9;padding:24px;'>" +
                "<table width='100%' cellpadding='0' cellspacing='0' style='max-width:600px;margin:auto;background:#ffffff;border-radius:8px;padding:24px;'>" +
                "<tr><td>" +
                "<h2 style='color:#2563eb;margin-top:0;'>HealthLink Account Decision</h2>" +
                "<p style='font-size:14px;color:#374151;'>Dear " + escape(user.getFullName()) + ",</p>" +
                decisionBlock +
                "<p style='font-size:14px;color:#6b7280;margin-top:32px;'>If you have questions, reply to this email." +
                "</p>" +
                "<p style='font-size:12px;color:#9ca3af;'>This is an automated message. © " + OffsetDateTime.now().getYear() + " HealthLink.</p>" +
                "</td></tr></table></body></html>";
    }

    private String approvedBlock() {
        return "<p style='font-size:14px;color:#374151;'>We are pleased to inform you that your account has been <strong style='color:#16a34a;'>approved</strong>. You may now sign in and access platform features.</p>" +
               "<p style='margin:20px 0;'><a href='https://app.healthlink.local/login' style='background:#2563eb;color:#ffffff;padding:12px 20px;text-decoration:none;border-radius:6px;font-size:14px;'>Access Your Account</a></p>";
    }

    private String rejectedBlock() {
        return "<p style='font-size:14px;color:#374151;'>After careful review your application has been <strong style='color:#dc2626;'>rejected</strong>." +
               " You may reply to this email for clarification or re-apply with updated information.</p>";
    }

    private String escape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
