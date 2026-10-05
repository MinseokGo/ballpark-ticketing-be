package com.ballpark.ticketing.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Section;
import com.ballpark.ticketing.game.dto.SeatBulkCreateRequest;
import com.ballpark.ticketing.game.dto.SeatBulkCreateResponse;
import com.ballpark.ticketing.game.repository.SeatRepository;
import com.ballpark.ticketing.game.repository.SectionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
class SeatServiceTest {

	@Autowired
	private SeatService seatService;

	@Autowired
	private SectionRepository sectionRepository;

	@Autowired
	private SeatRepository seatRepository;

	@Test
	void createsSeatsForEveryRowAndColumn() {
		Section section = sectionRepository.save(new Section("Outfield 301", "B", 15_000));

		SeatBulkCreateResponse response = seatService.createBulk(section.getId(), new SeatBulkCreateRequest(10, 20));

		assertThat(response.sectionId()).isEqualTo(section.getId());
		assertThat(response.createdCount()).isEqualTo(200);
		assertThat(seatRepository.count()).isEqualTo(200);
	}

	@Test
	void rejectsUnknownSection() {
		assertThatThrownBy(() -> seatService.createBulk(999_999L, new SeatBulkCreateRequest(1, 1)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.SECTION_NOT_FOUND);
	}
}
