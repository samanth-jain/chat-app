package com.example.chatapp.chat.exception;

public class ParticipantNotInRoomException extends RuntimeException {

	public ParticipantNotInRoomException(String participant, String roomId) {
		super("User '" + participant + "' must join chat room '" + roomId + "' before sending messages.");
	}
}
