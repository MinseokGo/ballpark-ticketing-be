package com.ballpark.ticketing.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Spring Data의 {@code Page}를 그대로 내려주면 pageable·sort 등 내부 구현이 그대로 노출된다.
 * 응답에 필요한 필드만 담은 DTO로 감싼다.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <T> PageResponse<T> from(Page<T> page) {
		return new PageResponse<>(
				page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
