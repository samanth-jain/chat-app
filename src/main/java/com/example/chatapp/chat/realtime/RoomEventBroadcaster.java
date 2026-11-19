package com.example.chatapp.chat.realtime;

import com.example.chatapp.chat.model.ChatMessage;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class RoomEventBroadcaster {

	private final ConcurrentHashMap<String, Set<SseEmitter>> emittersByRoom = new ConcurrentHashMap<>();

	public SseEmitter subscribe(String roomId) {
		SseEmitter emitter = new SseEmitter(0L);
		emittersByRoom.compute(roomId, (ignored, emitters) -> {
			Set<SseEmitter> roomEmitters = emitters == null ? ConcurrentHashMap.newKeySet() : emitters;
			roomEmitters.add(emitter);
			return roomEmitters;
		});
		emitter.onCompletion(() -> remove(roomId, emitter));
		emitter.onTimeout(() -> remove(roomId, emitter));
		emitter.onError(ignored -> remove(roomId, emitter));
		return emitter;
	}

	public void broadcast(String roomId, ChatMessage message) {
		Set<SseEmitter> emitters = emittersByRoom.get(roomId);
		if (emitters == null) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(SseEmitter.event().name("message").data(message));
			} catch (IOException exception) {
				try {
					emitter.completeWithError(exception);
				} catch (IllegalStateException ignored) {
					// The connection may have completed while the send failed.
				}
				remove(roomId, emitter);
			} catch (IllegalStateException exception) {
				remove(roomId, emitter);
			}
		}
	}

	private void remove(String roomId, SseEmitter emitter) {
		emittersByRoom.computeIfPresent(roomId, (ignored, emitters) -> {
			emitters.remove(emitter);
			return emitters.isEmpty() ? null : emitters;
		});
	}
}
