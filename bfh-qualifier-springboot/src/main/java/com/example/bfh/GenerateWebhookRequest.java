package com.example.bfh;

public record GenerateWebhookRequest(
        String fullName,
        String rollNumber,
        String email
) {
}
