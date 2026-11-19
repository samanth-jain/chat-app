package com.example.chatapp.chat.service;

import com.example.chatapp.chat.exception.ChatRoomAlreadyExistsException;
import com.example.chatapp.chat.exception.ChatRoomNotFoundException;
import com.example.chatapp.chat.exception.InvalidHistoryLimitException;
import com.example.chatapp.chat.exception.ParticipantNotInRoomException;
import com.example.chatapp.chat.model.ChatMessage;
import com.example.chatapp.chat.repository.ChatRoomRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatRoomService {

	private static final int MAX_HISTORY_LIMIT = 100;

	private final ChatRoomRepository repository;

	public ChatRoomService(ChatRoomRepository repository) {
		this.repository = repository;
	}

	public void createRoom(String roomId) {
		if (!repository.createRoom(roomId)) {
			throw new ChatRoomAlreadyExistsException(roomId);
		}
	}

	public void joinRoom(String roomId, String participant) {
		requireRoom(roomId);
		repository.addParticipant(roomId, participant);
	}

	public void sendMessage(String roomId, String participant, String text) {
		requireRoom(roomId);
		if (!repository.hasParticipant(roomId, participant)) {
			throw new ParticipantNotInRoomException(participant, roomId);
		}
		repository.appendMessage(roomId, new ChatMessage(participant, text, Instant.now()));
	}

	public List<ChatMessage> getLatestMessages(String roomId, int limit) {
		requireRoom(roomId);
		if (limit < 1 || limit > MAX_HISTORY_LIMIT) {
			throw new InvalidHistoryLimitException(MAX_HISTORY_LIMIT);
		}
		return repository.getLatestMessages(roomId, limit);
	}

	public void ensureRoomExists(String roomId) {
		requireRoom(roomId);
	}

	private void requireRoom(String roomId) {
		if (!repository.roomExists(roomId)) {
			throw new ChatRoomNotFoundException(roomId);
		}
	}
}
