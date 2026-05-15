package com.auction.server.service;

public class EmailService {
    public static void sendEmailAsync(String to, String subject, String body) {
        System.out.println("[EmailService] Gửi email đến: " + to);
        System.out.println("[EmailService] Chủ đề: " + subject);
        // Trong thực tế sẽ gửi qua SMTP server
    }
}
