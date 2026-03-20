package com.company.grc.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Async;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendNewGstNotification(String gstin) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("scrapbiddate@gmail.com");
            message.setTo(
                    "dkggroup@gmail.com",
                    "contact@scrapdms.com", "harshiscoding@gmail.com");

            message.setSubject("New Vendor Registration Added: " + gstin);
            message.setText("A new Vendor Registration entry has been added for GSTIN: " + gstin +
                    "\n\nYou can view and update the details in the GRC dashboard.");

            mailSender.send(message);
            System.out.println("Email notification sent for GSTIN: " + gstin);
        } catch (Exception e) {
            System.err.println("Failed to send email notification for GSTIN: " + gstin + ". Error: " + e.getMessage());
        }
    }
}
