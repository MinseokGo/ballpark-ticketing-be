package com.ballpark.ticketing.game.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.dto.SectionResponse;
import com.ballpark.ticketing.game.service.SectionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminSectionController.class)
class AdminSectionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SectionService sectionService;

	@Test
	void createsSection() throws Exception {
		given(sectionService.create(any())).willReturn(new SectionResponse(1L, "Infield 101", "R", 30_000));

		mockMvc.perform(post("/api/admin/sections")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Infield 101", "grade": "R", "price": 30000}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.name").value("Infield 101"));
	}

	@Test
	void rejectsBlankName() throws Exception {
		mockMvc.perform(post("/api/admin/sections")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "", "grade": "R", "price": 30000}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-001"));
	}

	@Test
	void duplicateNameReturnsConflict() throws Exception {
		given(sectionService.create(any())).willThrow(new BusinessException(ErrorCode.SECTION_NAME_DUPLICATE));

		mockMvc.perform(post("/api/admin/sections")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "Infield 101", "grade": "R", "price": 30000}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("SEAT-006"));
	}
}
