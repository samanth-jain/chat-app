package com.example.chatapp.chat.exception;

public class InvalidHistoryLimitException extends RuntimeException {

	public InvalidHistoryLimitException(int maximum) {
		super("The history limit must be between 1 and " + maximum + ".");
	}
}
