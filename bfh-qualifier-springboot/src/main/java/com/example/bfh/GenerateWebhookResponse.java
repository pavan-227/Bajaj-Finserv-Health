package com.example.bfh;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GenerateWebhookResponse(
        @JsonProperty("webhook_url") String webhookUrl,
        @JsonProperty("token") String token
) {
}
