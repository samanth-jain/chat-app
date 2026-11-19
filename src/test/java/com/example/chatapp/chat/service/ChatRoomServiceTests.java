package com.example.chatapp.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.chatapp.chat.exception.ChatRoomAlreadyExistsException;
import com.example.chatapp.chat.exception.ChatRoomNotFoundException;
import com.example.chatapp.chat.exception.InvalidHistoryLimitException;
import com.example.chatapp.chat.exception.ParticipantNotInRoomException;
import com.example.chatapp.chat.model.ChatMessage;
import com.example.chatapp.chat.repository.ChatRoomRepository;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTests {

	@Mock
	private ChatRoomRepository repository;

	private ChatRoomService service;

	@BeforeEach
	void setUp() {
		service = new ChatRoomService(repository);
	}

	@Test
	void rejectsDuplicateRoomNames() {
		when(repository.createRoom("general")).thenReturn(false);

		assertThrows(ChatRoomAlreadyExistsException.class, () -> service.createRoom("general"));
	}

	@Test
	void rejectsJoiningUnknownRoom() {
		when(repository.roomExists("missing")).thenReturn(false);

		assertThrows(ChatRoomNotFoundException.class, () -> service.joinRoom("missing", "guest_user"));
		verify(repository, never()).addParticipant(any(), any());
	}

	@Test
	void rejectsMessagesForUnknownRooms() {
		when(repository.roomExists("missing")).thenReturn(false);

		assertThrows(
				ChatRoomNotFoundException.class,
				() -> service.sendMessage("missing", "guest_user", "Hello"));
		verify(repository, never()).hasParticipant(any(), any());
	}

	@Test
	void requiresMembershipBeforeSendingMessages() {
		when(repository.roomExists("general")).thenReturn(true);
		when(repository.hasParticipant("general", "guest_user")).thenReturn(false);

		assertThrows(
				ParticipantNotInRoomException.class,
				() -> service.sendMessage("general", "guest_user", "Hello"));
		verify(repository, never()).appendMessage(any(), any());
	}

	@Test
	void appendsMessagesFromJoinedParticipantsWithTimestamps() {
		when(repository.roomExists("general")).thenReturn(true);
		when(repository.hasParticipant("general", "guest_user")).thenReturn(true);

		service.sendMessage("general", "guest_user", "Hello");

		verify(repository).appendMessage(eq("general"), argThat(message ->
				message.participant().equals("guest_user")
						&& message.message().equals("Hello")
						&& message.timestamp() != null));
	}

	@Test
	void returnsHistoryInRepositoryOrderWithRequestedLimit() {
		List<ChatMessage> expected = List.of(
				new ChatMessage("guest_user", "Hello", Instant.parse("2024-01-01T10:00:00Z")));
		when(repository.roomExists("general")).thenReturn(true);
		when(repository.getLatestMessages("general", 10)).thenReturn(expected);

		assertEquals(expected, service.getLatestMessages("general", 10));
	}

	@Test
	void rejectsHistoryLimitsOutsideSupportedRange() {
		when(repository.roomExists("general")).thenReturn(true);

		assertThrows(InvalidHistoryLimitException.class, () -> service.getLatestMessages("general", 101));
		verify(repository, never()).getLatestMessages(any(), any(Integer.class));
	}
}
