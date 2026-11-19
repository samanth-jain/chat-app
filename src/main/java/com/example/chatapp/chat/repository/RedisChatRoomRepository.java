package com.example.chatapp.chat.repository;

import com.example.chatapp.chat.exception.ChatRoomNotFoundException;
import com.example.chatapp.chat.model.ChatMessage;
import java.util.List;
import java.util.Objects;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Repository
public class RedisChatRoomRepository implements ChatRoomRepository {

	private static final String ROOM_PREFIX = "chat:room:";
	private static final String CREATE_ROOM_SCRIPT = """
			if redis.call('EXISTS', KEYS[1]) == 1 then
				return 0
			end
			redis.call('HSET', KEYS[1], 'roomId', ARGV[1], 'createdAt', ARGV[2])
			return 1
			""";
	private static final String APPEND_MESSAGE_SCRIPT = """
			if redis.call('EXISTS', KEYS[1]) == 0 then
				return 0
			end
			redis.call('RPUSH', KEYS[2], ARGV[1])
			redis.call('PUBLISH', KEYS[3], ARGV[1])
			return 1
			""";
	private static final DefaultRedisScript<Long> CREATE_ROOM =
			new DefaultRedisScript<>(CREATE_ROOM_SCRIPT, Long.class);
	private static final DefaultRedisScript<Long> APPEND_MESSAGE =
			new DefaultRedisScript<>(APPEND_MESSAGE_SCRIPT, Long.class);

	private final StringRedisTemplate redis;
	private final ObjectMapper objectMapper;

	public RedisChatRoomRepository(StringRedisTemplate redis, ObjectMapper objectMapper) {
		this.redis = redis;
		this.objectMapper = objectMapper;
	}

	@Override
	public boolean createRoom(String roomId) {
		Long created = redis.execute(
				CREATE_ROOM,
				List.of(metadataKey(roomId)),
				roomId,
				java.time.Instant.now().toString());
		return requireScriptResult(created) == 1L;
	}

	@Override
	public boolean roomExists(String roomId) {
		Boolean exists = redis.hasKey(metadataKey(roomId));
		if (exists == null) {
			throw new IllegalStateException("Redis did not return the room existence result.");
		}
		return exists;
	}

	@Override
	public void addParticipant(String roomId, String participant) {
		redis.opsForSet().add(participantsKey(roomId), participant);
	}

	@Override
	public boolean hasParticipant(String roomId, String participant) {
		Boolean member = redis.opsForSet().isMember(participantsKey(roomId), participant);
		if (member == null) {
			throw new IllegalStateException("Redis did not return the participant membership result.");
		}
		return member;
	}

	@Override
	public void appendMessage(String roomId, ChatMessage message) {
		Long appended = redis.execute(
				APPEND_MESSAGE,
				List.of(metadataKey(roomId), messagesKey(roomId), eventsChannel(roomId)),
				writeMessage(message));
		if (requireScriptResult(appended) != 1L) {
			throw new ChatRoomNotFoundException(roomId);
		}
	}

	@Override
	public List<ChatMessage> getLatestMessages(String roomId, int limit) {
		List<String> storedMessages = redis.opsForList().range(messagesKey(roomId), -limit, -1);
		if (storedMessages == null) {
			throw new IllegalStateException("Redis did not return the chat history.");
		}
		return storedMessages.stream().map(this::readMessage).toList();
	}

	private String writeMessage(ChatMessage message) {
		try {
			return objectMapper.writeValueAsString(message);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Could not serialize the chat message.", exception);
		}
	}

	private ChatMessage readMessage(String json) {
		try {
			return objectMapper.readValue(json, ChatMessage.class);
		} catch (JacksonException exception) {
			throw new IllegalStateException("Could not deserialize a stored chat message.", exception);
		}
	}

	private long requireScriptResult(Long result) {
		return Objects.requireNonNull(result, "Redis did not return the script result.");
	}

	private String metadataKey(String roomId) {
		return roomKey(roomId) + ":meta";
	}

	private String participantsKey(String roomId) {
		return roomKey(roomId) + ":participants";
	}

	private String messagesKey(String roomId) {
		return roomKey(roomId) + ":messages";
	}

	private String eventsChannel(String roomId) {
		return roomKey(roomId) + ":events";
	}

	private String roomKey(String roomId) {
		return ROOM_PREFIX + "{" + roomId + "}";
	}
}
