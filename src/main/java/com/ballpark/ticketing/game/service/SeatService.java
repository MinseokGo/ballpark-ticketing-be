package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.SeatPosition;
import com.ballpark.ticketing.game.dto.SeatBulkCreateRequest;
import com.ballpark.ticketing.game.dto.SeatBulkCreateResponse;
import com.ballpark.ticketing.game.repository.SeatJdbcRepository;
import com.ballpark.ticketing.game.repository.SectionRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SeatService {

	private final SectionRepository sectionRepository;
	private final SeatJdbcRepository seatJdbcRepository;

	public SeatService(SectionRepository sectionRepository, SeatJdbcRepository seatJdbcRepository) {
		this.sectionRepository = sectionRepository;
		this.seatJdbcRepository = seatJdbcRepository;
	}

	public SeatBulkCreateResponse createBulk(Long sectionId, SeatBulkCreateRequest request) {
		if (!sectionRepository.existsById(sectionId)) {
			throw new BusinessException(ErrorCode.SECTION_NOT_FOUND);
		}
		List<SeatPosition> positions = new ArrayList<>(request.rowCount() * request.seatsPerRow());
		for (int rowNo = 1; rowNo <= request.rowCount(); rowNo++) {
			for (int seatNo = 1; seatNo <= request.seatsPerRow(); seatNo++) {
				positions.add(new SeatPosition(rowNo, seatNo));
			}
		}
		int createdCount = seatJdbcRepository.batchInsert(sectionId, positions);
		return new SeatBulkCreateResponse(sectionId, createdCount);
	}
}
