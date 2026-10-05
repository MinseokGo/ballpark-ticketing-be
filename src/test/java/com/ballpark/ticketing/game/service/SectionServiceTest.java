package com.ballpark.ticketing.game.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.ballpark.ticketing.TestcontainersConfiguration;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.dto.SectionCreateRequest;
import com.ballpark.ticketing.game.dto.SectionResponse;
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
class SectionServiceTest {

	@Autowired
	private SectionService sectionService;

	@Test
	void createsSectionAndReturnsResponse() {
		SectionResponse response = sectionService.create(new SectionCreateRequest("Infield 201", "R", 25_000));

		assertThat(response.id()).isNotNull();
		assertThat(response.name()).isEqualTo("Infield 201");
		assertThat(response.grade()).isEqualTo("R");
		assertThat(response.price()).isEqualTo(25_000);
	}

	@Test
	void rejectsDuplicateSectionName() {
		sectionService.create(new SectionCreateRequest("Infield 202", "R", 25_000));

		assertThatThrownBy(() -> sectionService.create(new SectionCreateRequest("Infield 202", "S", 20_000)))
				.isInstanceOf(BusinessException.class)
				.extracting(e -> ((BusinessException) e).getErrorCode())
				.isEqualTo(ErrorCode.SECTION_NAME_DUPLICATE);
	}
}
