package com.example.chatapp.chat.realtime;

import com.example.chatapp.chat.model.ChatMessage;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.Message;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class RedisChatEventListenerTests {

	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private RoomEventBroadcaster eventBroadcaster;

	@Test
	void forwardsPublishedRoomMessagesToTheLocalSseSubscribers() throws Exception {
		ChatMessage message = new ChatMessage("guest_user", "Hello", Instant.parse("2024-01-01T10:00:00Z"));
		Message redisMessage = mock(Message.class);
		when(redisMessage.getChannel()).thenReturn("chat:room:{general}:events".getBytes(StandardCharsets.UTF_8));
		when(redisMessage.getBody()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));
		when(objectMapper.readValue(any(byte[].class), eq(ChatMessage.class))).thenReturn(message);

		new RedisChatEventListener(objectMapper, eventBroadcaster).onMessage(redisMessage, null);

		verify(eventBroadcaster).broadcast("general", message);
	}
}
