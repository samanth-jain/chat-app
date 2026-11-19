package com.example.chatapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.chatapp.chat.model.ChatMessage;
import com.example.chatapp.chat.repository.RedisChatRoomRepository;
import com.example.chatapp.chat.realtime.RedisChatEventListener;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
class ChatappApplicationTests {

	@MockitoBean
	private RedisMessageListenerContainer redisMessageListenerContainer;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RedisChatRoomRepository repository;

	@Autowired
	private RedisChatEventListener eventListener;

	@Test
	void applicationContextProvidesMapperForRedisComponents() {
		ChatMessage message = new ChatMessage("guest_user", "Hello", Instant.parse("2024-01-01T10:00:00Z"));

		String json = objectMapper.writeValueAsString(message);

		assertTrue(json.contains("\"timestamp\":\"2024-01-01T10:00:00Z\""));
		assertEquals(message, objectMapper.readValue(json, ChatMessage.class));
	}
}
