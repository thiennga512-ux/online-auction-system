package com.auction.server.service;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

/**
 * ============================================================
 * Class EmailService
 * ============================================================
 * Dịch vụ xử lý việc gửi email thông báo sử dụng Jakarta Mail.
 * Chạy bất đồng bộ để không block luồng xử lý chính của Server.
 * 
 * LƯU Ý: Cần cấu hình EMAIL_USERNAME và EMAIL_PASSWORD (App Password).
 * ============================================================
 */
public class EmailService {

  // Cấu hình tài khoản gửi mail (Nên đưa vào file config/env, tạm thời hardcode
  // hoặc dùng biến môi trường)
  private static final String SMTP_HOST = "smtp.gmail.com";
  private static final String SMTP_PORT = "587";

  private static final String EMAIL_USERNAME = "your_email@gmail.com";
  private static final String EMAIL_PASSWORD = "your_app_password";

  // Sử dụng thread pool riêng biệt cho việc gửi mail để tránh làm chậm server
  private static final ExecutorService emailExecutor = Executors.newFixedThreadPool(2);

  /**
   * Kiểm tra xem EmailService có đang chạy ở chế độ giả (MOCK) không.
   * Mock mode xảy ra khi chưa điền email/mật khẩu thật.
   */
  public static boolean isMockMode() {
    return "your_email@gmail.com".equals(EMAIL_USERNAME);
  }

  /**
   * Gửi email bất đồng bộ.
   *
   * @param toAddress Địa chỉ email người nhận
   * @param subject   Tiêu đề email
   * @param body      Nội dung email
   */
  public static void sendEmailAsync(String toAddress, String subject, String body) {
    if (toAddress == null || toAddress.trim().isEmpty()) {
      return;
    }

    // Nếu chưa cấu hình email thật, log ra console thay vì gửi lỗi
    if ("your_email@gmail.com".equals(EMAIL_USERNAME)) {
      System.out.println("[EmailService - MOCK] Sẽ gửi email tới: " + toAddress);
      System.out.println("   Subject: " + subject);
      System.out.println("   Body: " + body.replace("\n", " | "));
      return;
    }

    emailExecutor.submit(() -> {
      try {
        System.out.println("[EmailService] Đang chuẩn bị gửi email tới: " + toAddress);
        sendEmailSync(toAddress, subject, body);
        System.out.println("[EmailService] Đã gửi email thành công tới: " + toAddress);
      } catch (Exception e) {
        System.err.println("[EmailService] Lỗi khi gửi email tới " + toAddress + ": " + e.getMessage());
      }
    });
  }

  /**
   * Hàm thực hiện logic gửi email thực tế sử dụng Jakarta Mail.
   */
  private static void sendEmailSync(String toAddress, String subject, String body) throws MessagingException {
    Properties props = new Properties();
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true");
    props.put("mail.smtp.host", SMTP_HOST);
    props.put("mail.smtp.port", SMTP_PORT);

    Session session = Session.getInstance(props, new Authenticator() {
      @Override
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD);
      }
    });

    Message message = new MimeMessage(session);
    message.setFrom(new InternetAddress(EMAIL_USERNAME));
    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
    message.setSubject(subject);
    message.setText(body);

    Transport.send(message);
  }
}
