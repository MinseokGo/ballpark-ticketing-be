package com.ballpark.ticketing.reservation.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.ballpark.ticketing.common.auth.LoginUserArgumentResolver;
import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.reservation.ReservationStatus;
import com.ballpark.ticketing.reservation.dto.ReservationResponse;
import com.ballpark.ticketing.reservation.service.ReservationService;
import com.ballpark.ticketing.user.JwtService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReservationController.class)
@Import({LoginUserArgumentResolver.class, JwtService.class})
class ReservationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ReservationService reservationService;

	@Autowired
	private JwtService jwtService;

	private String bearer() {
		return "Bearer " + jwtService.issue(10L);
	}

	@Test
	void createsAReservation() throws Exception {
		ReservationResponse response = new ReservationResponse(
				1L, 10L, 1L, ReservationStatus.PENDING, 40_000, List.of(1L, 2L), LocalDateTime.now());
		given(reservationService.create(eq(1L), eq(10L), any())).willReturn(response);

		mockMvc.perform(post("/api/games/1/reservations")
						.header("Authorization", bearer())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"gameSeatIds": [1, 2]}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PENDING"))
				.andExpect(jsonPath("$.totalPrice").value(40_000));
	}

	@Test
	void rejectsARequestWithoutAToken() throws Exception {
		mockMvc.perform(post("/api/games/1/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"gameSeatIds": [1, 2]}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("AUTH-001"));
	}

	@Test
	void rejectsMoreThanFourSeats() throws Exception {
		mockMvc.perform(post("/api/games/1/reservations")
						.header("Authorization", bearer())
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
						.header("Authorization", bearer())
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
		given(reservationService.cancel(eq(1L), anyLong())).willReturn(response);

		mockMvc.perform(post("/api/reservations/1/cancel").header("Authorization", bearer()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));
	}
}
