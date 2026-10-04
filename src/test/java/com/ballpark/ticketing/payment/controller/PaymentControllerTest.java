package com.ballpark.ticketing.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ballpark.ticketing.common.exception.BusinessException;
import com.ballpark.ticketing.common.exception.ErrorCode;
import com.ballpark.ticketing.payment.PaymentStatus;
import com.ballpark.ticketing.payment.dto.PaymentResponse;
import com.ballpark.ticketing.payment.service.PaymentService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
class PaymentControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PaymentService paymentService;

	@Test
	void paysForAReservation() throws Exception {
		given(paymentService.pay(eq(1L), any()))
				.willReturn(new PaymentResponse(1L, 1L, 40_000, PaymentStatus.PAID, LocalDateTime.now()));

		mockMvc.perform(post("/api/reservations/1/payments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"success": true}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PAID"));
	}

	@Test
	void rejectsMissingSuccessField() throws Exception {
		mockMvc.perform(post("/api/reservations/1/payments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-001"));
	}

	@Test
	void reservationNotPendingReturnsConflict() throws Exception {
		given(paymentService.pay(eq(1L), any()))
				.willThrow(new BusinessException(ErrorCode.RESERVATION_NOT_PENDING));

		mockMvc.perform(post("/api/reservations/1/payments")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"success": true}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RESERVATION-002"));
	}
}
