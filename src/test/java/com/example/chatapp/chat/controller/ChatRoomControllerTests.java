package com.example.chatapp.chat.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.chatapp.chat.exception.ApiExceptionHandler;
import com.example.chatapp.chat.exception.ChatRoomAlreadyExistsException;
import com.example.chatapp.chat.model.ChatMessage;
import com.example.chatapp.chat.realtime.RoomEventBroadcaster;
import com.example.chatapp.chat.service.ChatRoomService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ChatRoomControllerTests {

	@Mock
	private ChatRoomService chatRoomService;

	@Mock
	private RoomEventBroadcaster eventBroadcaster;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new ChatRoomController(chatRoomService, eventBroadcaster))
				.setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	void createsRoomWithAssignmentResponseShape() throws Exception {
		mockMvc.perform(post("/api/chatapp/chatrooms")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"roomName\":\"general\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.roomId").value("general"))
				.andExpect(jsonPath("$.status").value("success"))
				.andExpect(jsonPath("$.message").value("Chat room 'general' created successfully."));

		verify(chatRoomService).createRoom("general");
	}

	@Test
	void returnsBadRequestForInvalidRoomName() throws Exception {
		mockMvc.perform(post("/api/chatapp/chatrooms")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"roomName\":\"bad room\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.message").isNotEmpty());

		verifyNoInteractions(chatRoomService);
	}

	@Test
	void joinsRoomWithAssignmentResponseShape() throws Exception {
		mockMvc.perform(post("/api/chatapp/chatrooms/general/join")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"participant\":\"guest_user\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("success"))
				.andExpect(jsonPath("$.message").value("User 'guest_user' joined chat room 'general'."));

		verify(chatRoomService).joinRoom("general", "guest_user");
	}

	@Test
	void returnsConflictForDuplicateRoom() throws Exception {
		doThrow(new ChatRoomAlreadyExistsException("general"))
				.when(chatRoomService).createRoom("general");

		mockMvc.perform(post("/api/chatapp/chatrooms")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"roomName\":\"general\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.message").value("Chat room 'general' already exists."));
	}

	@Test
	void sendsMessageUsingTheAssignmentContract() throws Exception {
		mockMvc.perform(post("/api/chatapp/chatrooms/general/messages")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"participant\":\"guest_user\",\"message\":\"Hello, everyone!\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Message sent successfully."))
				.andExpect(jsonPath("$.status").value("success"));

		verify(chatRoomService).sendMessage("general", "guest_user", "Hello, everyone!");
	}

	@Test
	void retrievesHistoryInTheSpecifiedResponseShape() throws Exception {
		when(chatRoomService.getLatestMessages("general", 10)).thenReturn(List.of(
				new ChatMessage("guest_user", "Hello", Instant.parse("2024-01-01T10:00:00Z"))));

		mockMvc.perform(get("/api/chatapp/chatrooms/general/messages").param("limit", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.messages[0].participant").value("guest_user"))
				.andExpect(jsonPath("$.messages[0].message").value("Hello"))
				.andExpect(jsonPath("$.messages[0].timestamp").value("2024-01-01T10:00:00Z"));
	}
}
