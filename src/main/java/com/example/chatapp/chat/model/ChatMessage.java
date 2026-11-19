package com.example.chatapp.chat.model;

import java.time.Instant;

public record ChatMessage(String participant, String message, Instant timestamp) {
}
