package com.bank.api.dto;

import com.bank.api.entity.OperationType;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record OperationItemResponse(
        OffsetDateTime date,
        OperationType type,
        BigDecimal amount,
        Long relatedUserId
) {
}
