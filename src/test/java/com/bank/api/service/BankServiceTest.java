package com.bank.api.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bank.api.dto.BalanceResponse;
import com.bank.api.dto.OperationItemResponse;
import com.bank.api.entity.Account;
import com.bank.api.entity.Operation;
import com.bank.api.entity.OperationType;
import com.bank.api.exception.BusinessException;
import com.bank.api.repository.AccountRepository;
import com.bank.api.repository.OperationRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private OperationRepository operationRepository;

    @InjectMocks
    private BankService bankService;

    private Account account(long userId, String balance) {
        Account account = new Account();
        account.setUserId(userId);
        account.setBalance(new BigDecimal(balance));
        return account;
    }

    @Test
    void getBalanceReturnsCurrentBalance() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(1L, "1000.00")));

        BalanceResponse response = bankService.getBalance(1L);

        assertEquals(1L, response.userId());
        assertEquals(0, response.balance().compareTo(new BigDecimal("1000.00")));
    }

    @Test
    void getBalanceThrowsWhenUserNotFound() {
        when(accountRepository.findById(100L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> bankService.getBalance(100L));

        assertEquals(-1, ex.getCode());
        assertEquals("user not found", ex.getMessage());
    }

    @Test
    void withdrawUpdatesBalanceAndCreatesOperation() {
        Account account = account(1L, "100.00");
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account));

        bankService.withdraw(1L, new BigDecimal("30.00"));

        assertEquals(0, account.getBalance().compareTo(new BigDecimal("70.00")));
        verify(accountRepository).save(account);

        ArgumentCaptor<Operation> operationCaptor = ArgumentCaptor.forClass(Operation.class);
        verify(operationRepository).save(operationCaptor.capture());
        Operation savedOperation = operationCaptor.getValue();
        assertEquals(1L, savedOperation.getUserId());
        assertEquals(OperationType.WITHDRAW, savedOperation.getOperationType());
        assertEquals(0, savedOperation.getAmount().compareTo(new BigDecimal("30.00")));
        assertEquals(null, savedOperation.getRelatedUserId());
        assertNotNull(savedOperation.getCreatedAt());
    }

    @Test
    void withdrawThrowsWhenInsufficientFunds() {
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account(1L, "10.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bankService.withdraw(1L, new BigDecimal("20.00")));

        assertEquals(0, ex.getCode());
        assertEquals("insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(operationRepository, never()).save(any(Operation.class));
    }

    @Test
    void depositUpdatesBalanceAndCreatesOperation() {
        Account account = account(1L, "100.00");
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account));

        bankService.deposit(1L, new BigDecimal("25.00"));

        assertEquals(0, account.getBalance().compareTo(new BigDecimal("125.00")));
        verify(accountRepository).save(account);

        ArgumentCaptor<Operation> operationCaptor = ArgumentCaptor.forClass(Operation.class);
        verify(operationRepository).save(operationCaptor.capture());
        Operation savedOperation = operationCaptor.getValue();
        assertEquals(1L, savedOperation.getUserId());
        assertEquals(OperationType.DEPOSIT, savedOperation.getOperationType());
        assertEquals(0, savedOperation.getAmount().compareTo(new BigDecimal("25.00")));
        assertEquals(null, savedOperation.getRelatedUserId());
        assertNotNull(savedOperation.getCreatedAt());
    }

    @Test
    void getOperationListReturnsMappedOperations() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(1L, "100.00")));
        OffsetDateTime from = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-12-31T23:59:59Z");

        Operation op = new Operation();
        op.setUserId(1L);
        op.setOperationType(OperationType.TRANSFER_OUT);
        op.setAmount(new BigDecimal("50.00"));
        op.setCreatedAt(OffsetDateTime.parse("2026-02-18T18:00:00Z"));
        op.setRelatedUserId(2L);
        when(operationRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(1L, from, to))
                .thenReturn(List.of(op));

        List<OperationItemResponse> result = bankService.getOperationList(1L, from, to);

        assertEquals(1, result.size());
        OperationItemResponse item = result.get(0);
        assertEquals(OperationType.TRANSFER_OUT, item.type());
        assertEquals(0, item.amount().compareTo(new BigDecimal("50.00")));
        assertEquals(2L, item.relatedUserId());
        assertEquals(OffsetDateTime.parse("2026-02-18T18:00:00Z"), item.date());
    }

    @Test
    void getOperationListUsesDefaultBoundsWhenDatesAreNull() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(1L, "100.00")));
        when(operationRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(eq(1L), any(), any()))
                .thenReturn(List.of());

        bankService.getOperationList(1L, null, null);

        ArgumentCaptor<OffsetDateTime> fromCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        ArgumentCaptor<OffsetDateTime> toCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(operationRepository).findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(eq(1L),
                fromCaptor.capture(), toCaptor.capture());

        assertEquals(OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), fromCaptor.getValue());
        assertTrue(!toCaptor.getValue().isBefore(fromCaptor.getValue()));
    }

    @Test
    void getOperationListThrowsWhenDateRangeInvalid() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account(1L, "100.00")));
        OffsetDateTime from = OffsetDateTime.parse("2026-12-31T23:59:59Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-01-01T00:00:00Z");

        BusinessException ex = assertThrows(BusinessException.class, () -> bankService.getOperationList(1L, from, to));

        assertEquals(0, ex.getCode());
        assertEquals("from must be before to", ex.getMessage());
        verify(operationRepository, never()).findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(any(), any(), any());
    }

    @Test
    void transferThrowsWhenSameUser() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> bankService.transfer(1L, 1L, new BigDecimal("10.00")));

        assertEquals(0, ex.getCode());
        assertEquals("cannot transfer to same user", ex.getMessage());
    }

    @Test
    void transferThrowsWhenNotEnoughFunds() {
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(account(1L, "10.00")));
        when(accountRepository.findByUserId(2L)).thenReturn(Optional.of(account(2L, "20.00")));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> bankService.transfer(1L, 2L, new BigDecimal("50.00")));

        assertEquals(0, ex.getCode());
        assertEquals("insufficient funds", ex.getMessage());
        verify(accountRepository, never()).save(any(Account.class));
        verify(operationRepository, never()).save(any(Operation.class));
    }

    @Test
    void transferMovesMoneyAndCreatesTwoOperations() {
        Account from = account(1L, "100.00");
        Account to = account(2L, "20.00");
        when(accountRepository.findByUserId(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findByUserId(2L)).thenReturn(Optional.of(to));

        bankService.transfer(1L, 2L, new BigDecimal("50.00"));

        assertEquals(0, from.getBalance().compareTo(new BigDecimal("50.00")));
        assertEquals(0, to.getBalance().compareTo(new BigDecimal("70.00")));
        verify(accountRepository, times(2)).save(any(Account.class));

        ArgumentCaptor<Operation> operationCaptor = ArgumentCaptor.forClass(Operation.class);
        verify(operationRepository, times(2)).save(operationCaptor.capture());
        List<Operation> saved = operationCaptor.getAllValues();

        Operation first = saved.get(0);
        Operation second = saved.get(1);
        assertEquals(OperationType.TRANSFER_OUT, first.getOperationType());
        assertEquals(1L, first.getUserId());
        assertEquals(2L, first.getRelatedUserId());

        assertEquals(OperationType.TRANSFER_IN, second.getOperationType());
        assertEquals(2L, second.getUserId());
        assertEquals(1L, second.getRelatedUserId());
    }
}
