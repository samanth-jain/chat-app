package com.example.chatapp.chat.dto;

import com.example.chatapp.chat.model.ChatMessage;
import java.util.List;

public record MessageHistoryResponse(List<ChatMessage> messages) {
}
