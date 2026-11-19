package com.example.chatapp.chat.realtime;

import com.example.chatapp.chat.model.ChatMessage;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class RedisChatEventListener implements MessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(RedisChatEventListener.class);
	private static final String CHANNEL_PREFIX = "chat:room:{";
	private static final String CHANNEL_SUFFIX = "}:events";

	private final ObjectMapper objectMapper;
	private final RoomEventBroadcaster eventBroadcaster;

	public RedisChatEventListener(ObjectMapper objectMapper, RoomEventBroadcaster eventBroadcaster) {
		this.objectMapper = objectMapper;
		this.eventBroadcaster = eventBroadcaster;
	}

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		if (!channel.startsWith(CHANNEL_PREFIX) || !channel.endsWith(CHANNEL_SUFFIX)) {
			LOGGER.warn("Ignoring message received on unexpected Redis channel '{}'.", channel);
			return;
		}

		String roomId = channel.substring(CHANNEL_PREFIX.length(), channel.length() - CHANNEL_SUFFIX.length());
		try {
			ChatMessage chatMessage = objectMapper.readValue(message.getBody(), ChatMessage.class);
			eventBroadcaster.broadcast(roomId, chatMessage);
		} catch (JacksonException exception) {
			LOGGER.warn("Ignoring malformed chat event published to room '{}'.", roomId, exception);
		}
	}
}
