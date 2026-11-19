package com.example.chatapp.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
		@NotBlank
		@Size(max = 64)
		@Pattern(regexp = "[A-Za-z0-9_.-]+", message = "must contain only letters, numbers, '_', '.' or '-'")
		String participant) {
}
