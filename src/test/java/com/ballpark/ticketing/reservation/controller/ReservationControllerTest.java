package com.ballpark.ticketing.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.reservation.ReservationStatus;
import com.ballpark.ticketing.reservation.dto.ReservationResponse;
import com.ballpark.ticketing.reservation.service.ReservationService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
class ReservationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReservationService reservationService;

	@Test
	void createsAReservation() throws Exception {
		ReservationResponse response = new ReservationResponse(
				1L, 10L, 1L, ReservationStatus.PENDING, 40_000, List.of(1L, 2L), LocalDateTime.now());
		given(reservationService.create(eq(1L), eq(10L), any())).willReturn(response);

		mockMvc.perform(post("/api/games/1/reservations")
						.header("X-User-Id", "10")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"gameSeatIds": [1, 2]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.totalPrice").value(40_000));
	}

	@Test
	void rejectsMissingUserIdHeader() throws Exception {
		mockMvc.perform(post("/api/games/1/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"gameSeatIds": [1, 2]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-001"));
	}

	@Test
	void rejectsMoreThanFourSeats() throws Exception {
		mockMvc.perform(post("/api/games/1/reservations")
						.header("X-User-Id", "10")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"gameSeatIds": [1, 2, 3, 4, 5]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-001"));
	}

	@Test
	void gameNotOpenReturnsConflict() throws Exception {
		given(reservationService.create(eq(1L), eq(10L), any()))
				.willThrow(new BusinessException(ErrorCode.GAME_NOT_OPEN));

		mockMvc.perform(post("/api/games/1/reservations")
						.header("X-User-Id", "10")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"gameSeatIds": [1]}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("GAME-003"));
	}

	@Test
	void cancelsAReservation() throws Exception {
		ReservationResponse response = new ReservationResponse(
				1L, 10L, 1L, ReservationStatus.CANCELLED, 40_000, List.of(1L, 2L), LocalDateTime.now());
		given(reservationService.cancel(1L)).willReturn(response);

		mockMvc.perform(post("/api/reservations/1/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}
}
