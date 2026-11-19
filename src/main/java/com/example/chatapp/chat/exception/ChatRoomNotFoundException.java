package com.example.chatapp.chat.exception;

public class ChatRoomNotFoundException extends RuntimeException {

	public ChatRoomNotFoundException(String roomId) {
		super("Chat room '" + roomId + "' was not found.");
	}
}
