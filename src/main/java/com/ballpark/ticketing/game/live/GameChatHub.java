package com.ballpark.ticketing.game.live;

import com.ballpark.ticketing.game.dto.ChatMessageResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** 경기별 채팅 SSE 연결. 중계 허브와 같은 방식이고, 이벤트 이름만 message다. 인스턴스 메모리에만 둔다. */
@Component
public class GameChatHub {

	private static final long CONNECTION_TIMEOUT_MS = 30 * 60 * 1000L;

	private final Map<Long, Set<SseEmitter>> emittersByGame = new ConcurrentHashMap<>();

	public SseEmitter subscribe(Long gameId) {
		SseEmitter emitter = new SseEmitter(CONNECTION_TIMEOUT_MS);
		emittersByGame.computeIfAbsent(gameId, key -> ConcurrentHashMap.newKeySet()).add(emitter);
		emitter.onCompletion(() -> remove(gameId, emitter));
		emitter.onTimeout(() -> remove(gameId, emitter));
		emitter.onError(error -> remove(gameId, emitter));
		return emitter;
	}

	public void sendTo(SseEmitter emitter, ChatMessageResponse message) {
		send(emitter, message);
	}

	/** 커밋 후에 구독자에게 보낸다. 롤백된 메시지는 나가지 않는다. */
	public void publishAfterCommit(Long gameId, ChatMessageResponse message) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			publish(gameId, message);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publish(gameId, message);
			}
		});
	}

	@Scheduled(fixedRate = 15_000)
	public void heartbeat() {
		emittersByGame.forEach((gameId, emitters) -> emitters.forEach(emitter -> {
			synchronized (emitter) {
				try {
					emitter.send(SseEmitter.event().comment("keep-alive"));
				} catch (IOException | IllegalStateException error) {
					remove(gameId, emitter);
				}
			}
		}));
	}

	private void publish(Long gameId, ChatMessageResponse message) {
		Set<SseEmitter> emitters = emittersByGame.get(gameId);
		if (emitters == null) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			send(emitter, message);
		}
	}

	private void send(SseEmitter emitter, ChatMessageResponse message) {
		synchronized (emitter) {
			try {
				emitter.send(SseEmitter.event().id(String.valueOf(message.id())).name("message").data(message));
			} catch (IOException | IllegalStateException error) {
				try {
					emitter.completeWithError(error);
				} catch (IllegalStateException ignored) {
					// 이미 닫힌 연결
				}
			}
		}
	}

	private void remove(Long gameId, SseEmitter emitter) {
		Set<SseEmitter> emitters = emittersByGame.get(gameId);
		if (emitters != null) {
			emitters.remove(emitter);
		}
	}
}
