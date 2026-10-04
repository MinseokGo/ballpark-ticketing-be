package com.ballpark.ticketing.game.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.game.dto.SeatBulkCreateResponse;
import com.ballpark.ticketing.game.service.SeatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AdminSeatController.class)
class AdminSeatControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SeatService seatService;

	@Test
	void createsSeatsInBulk() throws Exception {
		given(seatService.createBulk(eq(1L), any())).willReturn(new SeatBulkCreateResponse(1L, 200));

		mockMvc.perform(post("/api/admin/sections/1/seats")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"rowCount": 10, "seatsPerRow": 20}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sectionId").value(1))
				.andExpect(jsonPath("$.createdCount").value(200));
	}

	@Test
	void rejectsNonPositiveRowCount() throws Exception {
		mockMvc.perform(post("/api/admin/sections/1/seats")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"rowCount": 0, "seatsPerRow": 20}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-001"));
	}

	@Test
	void unknownSectionReturnsNotFound() throws Exception {
		given(seatService.createBulk(eq(999L), any()))
				.willThrow(new BusinessException(ErrorCode.SECTION_NOT_FOUND));

		mockMvc.perform(post("/api/admin/sections/999/seats")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"rowCount": 10, "seatsPerRow": 20}
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("SEAT-005"));
	}
}
