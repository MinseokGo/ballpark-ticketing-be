package com.ballpark.ticketing.common.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest({GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandlerTest.ValidatedTestController.class})
@Import({GlobalExceptionHandlerTest.TestController.class, GlobalExceptionHandlerTest.ValidatedTestController.class})
class GlobalExceptionHandlerTest {

	private static final MediaType PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON;

	@Autowired
	private MockMvc mockMvc;

	@Test
	void businessExceptionBecomesProblemDetailWithErrorCode() throws Exception {
		mockMvc.perform(post("/test/seats/1/hold"))
				.andExpect(status().isConflict())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.status").value(409))
				.andExpect(jsonPath("$.title").value("Conflict"))
				.andExpect(jsonPath("$.detail").value(ErrorCode.SEAT_NOT_AVAILABLE.getMessage()))
				.andExpect(jsonPath("$.instance").value("/test/seats/1/hold"))
				.andExpect(jsonPath("$.code").value("SEAT-002"));
	}

	@Test
	void invalidRequestBodyReturnsFieldErrors() throws Exception {
		mockMvc.perform(post("/test/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name": "", "seatIds": [1, 2, 3, 4, 5]}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
				.andExpect(jsonPath("$.code").value("COMMON-001"))
				.andExpect(jsonPath("$.detail").value(ErrorCode.INVALID_INPUT_VALUE.getMessage()))
				.andExpect(jsonPath("$.errors.length()").value(2))
				.andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
				.andExpect(jsonPath("$.errors[?(@.field == 'seatIds')]").exists());
	}

	@Test
	void invalidRequestParamReturnsFieldErrors() throws Exception {
		mockMvc.perform(get("/test/games").param("size", "1000"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-001"))
				.andExpect(jsonPath("$.errors[0].field").value("size"));
	}

	@Test
	void validatedBeanConstraintViolationReturnsFieldErrors() throws Exception {
		mockMvc.perform(get("/test/validated/games").param("size", "1000"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-001"))
				.andExpect(jsonPath("$.errors[0].field").value("size"));
	}

	@Test
	void malformedJsonReturnsBadRequest() throws Exception {
		mockMvc.perform(post("/test/reservations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("COMMON-001"))
				.andExpect(jsonPath("$.instance").value("/test/reservations"));
	}

	@Test
	void unsupportedMethodReturnsMethodNotAllowed() throws Exception {
		mockMvc.perform(delete("/test/reservations"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code").value("COMMON-004"));
	}

	@Test
	void unexpectedExceptionHidesInternalMessage() throws Exception {
		mockMvc.perform(get("/test/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("COMMON-999"))
				.andExpect(jsonPath("$.detail").value(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
	}

	@RestController
	static class TestController {

		@PostMapping("/test/seats/{id}/hold")
		void hold(@PathVariable Long id) {
			throw new BusinessException(ErrorCode.SEAT_NOT_AVAILABLE);
		}

		@PostMapping("/test/reservations")
		void reserve(@Valid @RequestBody ReservationRequest request) {
		}

		@GetMapping("/test/games")
		void games(@RequestParam @Max(100) int size) {
		}

		@GetMapping("/test/boom")
		void boom() {
			throw new IllegalStateException("internal detail that must not leak");
		}
	}

	@Validated
	@RestController
	static class ValidatedTestController {

		@GetMapping("/test/validated/games")
		void games(@RequestParam @Max(100) int size) {
		}
	}

	record ReservationRequest(@NotBlank String name, @Size(max = 4) List<Long> seatIds) {
	}
}
