package com.example.chatapp.chat.controller;

import com.example.chatapp.chat.dto.ActionResponse;
import com.example.chatapp.chat.dto.CreateRoomRequest;
import com.example.chatapp.chat.dto.CreateRoomResponse;
import com.example.chatapp.chat.dto.JoinRoomRequest;
import com.example.chatapp.chat.dto.MessageHistoryResponse;
import com.example.chatapp.chat.dto.SendMessageRequest;
import com.example.chatapp.chat.model.ChatMessage;
import com.example.chatapp.chat.realtime.RoomEventBroadcaster;
import com.example.chatapp.chat.service.ChatRoomService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chatapp/chatrooms")
public class ChatRoomController {

	private final ChatRoomService chatRoomService;
	private final RoomEventBroadcaster eventBroadcaster;

	public ChatRoomController(ChatRoomService chatRoomService, RoomEventBroadcaster eventBroadcaster) {
		this.chatRoomService = chatRoomService;
		this.eventBroadcaster = eventBroadcaster;
	}

	@PostMapping
	public CreateRoomResponse createRoom(@Valid @RequestBody CreateRoomRequest request) {
		chatRoomService.createRoom(request.roomName());
		return new CreateRoomResponse(
				"Chat room '" + request.roomName() + "' created successfully.",
				request.roomName(),
				"success");
	}

	@PostMapping("/{roomId}/join")
	public ActionResponse joinRoom(
			@PathVariable String roomId,
			@Valid @RequestBody JoinRoomRequest request) {
		chatRoomService.joinRoom(roomId, request.participant());
		return new ActionResponse(
				"User '" + request.participant() + "' joined chat room '" + roomId + "'.",
				"success");
	}

	@PostMapping("/{roomId}/messages")
	public ActionResponse sendMessage(
			@PathVariable String roomId,
			@Valid @RequestBody SendMessageRequest request) {
		chatRoomService.sendMessage(roomId, request.participant(), request.message());
		return new ActionResponse("Message sent successfully.", "success");
	}

	@GetMapping("/{roomId}/messages")
	public MessageHistoryResponse getMessages(
			@PathVariable String roomId,
			@RequestParam(defaultValue = "50") int limit) {
		List<ChatMessage> messages = chatRoomService.getLatestMessages(roomId, limit);
		return new MessageHistoryResponse(messages);
	}

	@GetMapping(path = "/{roomId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public ResponseEntity<SseEmitter> subscribeToRoom(@PathVariable String roomId) {
		chatRoomService.ensureRoomExists(roomId);
		return ResponseEntity.ok(eventBroadcaster.subscribe(roomId));
	}
}
