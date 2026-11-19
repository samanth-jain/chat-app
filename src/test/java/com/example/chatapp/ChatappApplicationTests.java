package com.example.chatapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.chatapp.chat.model.ChatMessage;
import com.example.chatapp.chat.repository.RedisChatRoomRepository;
import com.example.chatapp.chat.realtime.RedisChatEventListener;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
		"PORT=18089",
		"spring.data.redis.url=rediss://test-user:test-password@redis.example:6380",
		"spring.data.redis.ssl.enabled=true"
})
class ChatappApplicationTests {

	@MockitoBean
	private RedisMessageListenerContainer redisMessageListenerContainer;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private RedisChatRoomRepository repository;

	@Autowired
	private RedisChatEventListener eventListener;

	@Autowired
	private Environment environment;

	@Autowired
	private DataRedisConnectionDetails redisConnectionDetails;

	@Test
	void applicationContextProvidesMapperForRedisComponents() {
		ChatMessage message = new ChatMessage("guest_user", "Hello", Instant.parse("2024-01-01T10:00:00Z"));

		String json = objectMapper.writeValueAsString(message);

		assertTrue(json.contains("\"timestamp\":\"2024-01-01T10:00:00Z\""));
		assertEquals(message, objectMapper.readValue(json, ChatMessage.class));
	}

	@Test
	void deploymentPropertiesSelectPortAndRedisConnection() {
		assertEquals("18089", environment.getProperty("server.port"));
		assertEquals("redis.example", redisConnectionDetails.getStandalone().getHost());
		assertEquals(6380, redisConnectionDetails.getStandalone().getPort());
		assertEquals("test-user", redisConnectionDetails.getUsername());
		assertEquals("test-password", redisConnectionDetails.getPassword());
		assertNotNull(redisConnectionDetails.getSslBundle());
	}
}
