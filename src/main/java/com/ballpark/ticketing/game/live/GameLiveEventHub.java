package com.ballpark.ticketing.game.live;

import com.ballpark.ticketing.game.dto.LiveEventResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 경기별 SSE 연결을 메모리에 들고 이벤트를 밀어 준다. 인스턴스가 하나인 동안만 유효하다.
 * 여러 인스턴스로 가면 pub/sub을 그때 도입한다(설계 문서 4장).
 */
@Component
public class GameLiveEventHub {

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

	/** 연결 하나에 이벤트를 보낸다. 재접속 리플레이에 쓴다. 끝나는 이벤트면 연결을 닫는다. */
	public void sendTo(SseEmitter emitter, LiveEventResponse event) {
		send(emitter, event);
	}

	/** 이벤트를 커밋 이후에 구독자 전원에게 보낸다. 롤백된 이벤트는 나가지 않는다. */
	public void publishAfterCommit(Long gameId, LiveEventResponse event) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			publish(gameId, event);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publish(gameId, event);
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

	private void publish(Long gameId, LiveEventResponse event) {
		Set<SseEmitter> emitters = emittersByGame.get(gameId);
		if (emitters == null) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			send(emitter, event);
		}
	}

	private void send(SseEmitter emitter, LiveEventResponse event) {
		synchronized (emitter) {
			try {
				emitter.send(SseEmitter.event()
						.id(String.valueOf(event.seq()))
						.name(event.type().name())
						.data(event));
				if (event.isTerminal()) {
					emitter.complete();
				}
			} catch (IOException | IllegalStateException error) {
				// 이미 닫힌 연결이면 completeWithError도 실패할 수 있다. 구독 목록에서는 onError 콜백이 지운다.
				try {
					emitter.completeWithError(error);
				} catch (IllegalStateException ignored) {
					// 이미 완료된 연결
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
