package com.ballpark.ticketing.game.repository;

import com.ballpark.ticketing.game.GameSeat;
import com.ballpark.ticketing.game.dto.SeatMapItemResponse;
import com.ballpark.ticketing.game.dto.SectionAvailabilityResponse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GameSeatRepository extends JpaRepository<GameSeat, Long> {

	long countByGame_Id(Long gameId);

	/**
	 * 구역별 좌석 현황을 집계 쿼리 한 번으로 구한다(경기당 구역 수만큼 반복 조회하지 않는다).
	 */
	@Query("""
			select new com.ballpark.ticketing.game.dto.SectionAvailabilityResponse(
				s.id, s.name, s.grade, s.price,
				count(gs),
				sum(case when gs.status = com.ballpark.ticketing.game.GameSeatStatus.AVAILABLE then 1L else 0L end),
				sum(case when gs.status = com.ballpark.ticketing.game.GameSeatStatus.HELD then 1L else 0L end),
				sum(case when gs.status = com.ballpark.ticketing.game.GameSeatStatus.SOLD then 1L else 0L end))
			from GameSeat gs
			join gs.seat se
			join se.section s
			where gs.game.id = :gameId
			group by s.id, s.name, s.grade, s.price
			order by s.id
			""")
	List<SectionAvailabilityResponse> findSectionAvailabilityByGameId(@Param("gameId") Long gameId);

	/**
	 * 좌석맵 전체를 DTO 프로젝션 한 번으로 조회한다. Seat/Section을 엔티티로 받아 지연 로딩하면
	 * 좌석 수만큼 추가 쿼리가 나가는 N+1이 생기므로(docs/experiments/v1-03 참고), 처음부터
	 * join으로 필요한 컬럼만 선택한다.
	 */
	@Query("""
			select new com.ballpark.ticketing.game.dto.SeatMapItemResponse(
				gs.id, se.id, se.rowNo, se.seatNo, s.id, s.name, s.grade, gs.status)
			from GameSeat gs
			join gs.seat se
			join se.section s
			where gs.game.id = :gameId
			order by s.id, se.rowNo, se.seatNo
			""")
	List<SeatMapItemResponse> findSeatMapByGameId(@Param("gameId") Long gameId);
}
