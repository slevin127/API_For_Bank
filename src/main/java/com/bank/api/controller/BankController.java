package com.bank.api.controller;

import com.bank.api.dto.AmountRequest;
import com.bank.api.dto.ApiResponse;
import com.bank.api.dto.BalanceResponse;
import com.bank.api.dto.OperationItemResponse;
import com.bank.api.dto.TransferRequest;
import com.bank.api.service.BankService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bank")
@Validated
public class BankController {

    private final BankService bankService;

    public BankController(BankService bankService) {
        this.bankService = bankService;
    }

    @GetMapping("/getBalance")
    public ApiResponse<BalanceResponse> getBalance(@RequestParam @NotNull Long userId) {
        BalanceResponse data = bankService.getBalance(userId);
        return ApiResponse.ok(data.balance(), data);
    }

    @PostMapping("/takeMoney")
    public ApiResponse<Void> takeMoney(@RequestParam @NotNull Long userId, @Valid @RequestBody AmountRequest request) {
        bankService.withdraw(userId, request.amount());
        return ApiResponse.ok(1, null);
    }

    @PostMapping("/putMoney")
    public ApiResponse<Void> putMoney(@RequestParam @NotNull Long userId, @Valid @RequestBody AmountRequest request) {
        bankService.deposit(userId, request.amount());
        return ApiResponse.ok(1, null);
    }

    @GetMapping("/getOperationList")
    public ApiResponse<List<OperationItemResponse>> getOperationList(
            @RequestParam @NotNull Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        List<OperationItemResponse> operations = bankService.getOperationList(userId, from, to);
        return ApiResponse.ok(operations.size(), operations);
    }

    @PostMapping("/transferMoney")
    public ApiResponse<Void> transferMoney(@Valid @RequestBody TransferRequest request) {
        bankService.transfer(request.fromUserId(), request.toUserId(), request.amount());
        return ApiResponse.ok(1, null);
    }
}
