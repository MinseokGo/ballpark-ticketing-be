package com.ballpark.ticketing.payment.dto;

import jakarta.validation.constraints.NotNull;

/**
 * v1은 실제 PG 연동이 없는 Mock 결제라, 성공/실패를 호출하는 쪽이 직접 지정한다.
 */
public record PaymentCreateRequest(@NotNull Boolean success) {
}
