package com.ballpark.ticketing.game.live;

import com.ballpark.ticketing.game.dto.SeatSelectionResponse;
import com.ballpark.ticketing.game.dto.SeatStatusResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 경기별 좌석 상태 변경을 구독자에게 밀어 준다. 좌석 상태는 "현재 값"이라 재접속하면 좌석표를 다시 받는다(리플레이 없음).
 * 변경은 커밋 후에 보낸다. 인스턴스 메모리에만 두며, 여러 인스턴스는 pub/sub으로 그때 확장한다.
 */
@Component
public class SeatStatusHub {

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

	/** 좌석 변경을 커밋 후에 보낸다. 변경이 없으면 아무것도 보내지 않는다. */
	public void publishAfterCommit(Long gameId, List<SeatStatusResponse> changes) {
		if (changes.isEmpty()) {
			return;
		}
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			publish(gameId, changes);
			return;
		}
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				publish(gameId, changes);
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

	/** 지금 고르는 중인 좌석 목록을 모든 구독자에게 보낸다(예매 확정과는 무관한 표시용 신호). */
	public void publishSelections(Long gameId, List<SeatSelectionResponse> selections) {
		broadcast(gameId, "selection", selections);
	}

	/** 새로 연결한 구독자에게 지금의 고르는 중 목록을 한 번 보낸다. */
	public void sendSelections(SseEmitter emitter, List<SeatSelectionResponse> selections) {
		synchronized (emitter) {
			try {
				emitter.send(SseEmitter.event().name("selection").data(selections));
			} catch (IOException | IllegalStateException error) {
				// 연결이 이미 끊긴 경우는 다음 변경 때 정리된다
			}
		}
	}

	private void publish(Long gameId, List<SeatStatusResponse> changes) {
		broadcast(gameId, "seats", changes);
	}

	private void broadcast(Long gameId, String eventName, Object data) {
		Set<SseEmitter> emitters = emittersByGame.get(gameId);
		if (emitters == null) {
			return;
		}
		for (SseEmitter emitter : emitters) {
			synchronized (emitter) {
				try {
					emitter.send(SseEmitter.event().name(eventName).data(data));
				} catch (IOException | IllegalStateException error) {
					try {
						emitter.completeWithError(error);
					} catch (IllegalStateException ignored) {
						// 이미 닫힌 연결
					}
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
