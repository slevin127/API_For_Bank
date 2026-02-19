package com.bank.api.dto;

import java.math.BigDecimal;

public record BalanceResponse(Long userId, BigDecimal balance) {
}
