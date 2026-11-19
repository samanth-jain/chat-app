package com.example.chatapp.chat.exception;

public class ChatRoomAlreadyExistsException extends RuntimeException {

	public ChatRoomAlreadyExistsException(String roomId) {
		super("Chat room '" + roomId + "' already exists.");
	}
}
