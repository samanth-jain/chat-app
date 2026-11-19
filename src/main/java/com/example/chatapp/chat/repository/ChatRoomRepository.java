package com.example.chatapp.chat.repository;

import com.example.chatapp.chat.model.ChatMessage;
import java.util.List;

public interface ChatRoomRepository {

	boolean createRoom(String roomId);

	boolean roomExists(String roomId);

	void addParticipant(String roomId, String participant);

	boolean hasParticipant(String roomId, String participant);

	void appendMessage(String roomId, ChatMessage message);

	List<ChatMessage> getLatestMessages(String roomId, int limit);
}
