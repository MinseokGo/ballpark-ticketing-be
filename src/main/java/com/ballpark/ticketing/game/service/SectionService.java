package com.ballpark.ticketing.game.service;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.Section;
import com.ballpark.ticketing.game.dto.SectionCreateRequest;
import com.ballpark.ticketing.game.dto.SectionResponse;
import com.ballpark.ticketing.game.repository.SectionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class SectionService {

	private final SectionRepository sectionRepository;

	public SectionResponse create(SectionCreateRequest request) {
		if (sectionRepository.existsByName(request.name())) {
			throw new BusinessException(ErrorCode.SECTION_NAME_DUPLICATE);
		}
		Section section = new Section(request.name(), request.grade(), request.price());
		return SectionResponse.from(sectionRepository.save(section));
	}
}
