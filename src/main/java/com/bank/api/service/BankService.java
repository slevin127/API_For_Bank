package com.bank.api.service;

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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankService {

    private final AccountRepository accountRepository;
    private final OperationRepository operationRepository;

    public BankService(AccountRepository accountRepository, OperationRepository operationRepository) {
        this.accountRepository = accountRepository;
        this.operationRepository = operationRepository;
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId) {
        Account account = accountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(-1, "user not found"));
        return new BalanceResponse(userId, account.getBalance());
    }

    @Transactional
    public void withdraw(Long userId, BigDecimal amount) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(0, "user not found"));
        if (account.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(0, "insufficient funds");
        }
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        saveOperation(userId, OperationType.WITHDRAW, amount, null);
    }

    @Transactional
    public void deposit(Long userId, BigDecimal amount) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(0, "user not found"));
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        saveOperation(userId, OperationType.DEPOSIT, amount, null);
    }

    @Transactional(readOnly = true)
    public List<OperationItemResponse> getOperationList(Long userId, OffsetDateTime from, OffsetDateTime to) {
        accountRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(0, "user not found"));

        OffsetDateTime effectiveFrom = from == null
                ? OffsetDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
                : from;
        OffsetDateTime effectiveTo = to == null
                ? OffsetDateTime.now(ZoneOffset.UTC)
                : to;

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new BusinessException(0, "from must be before to");
        }

        return operationRepository.findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(userId, effectiveFrom, effectiveTo)
                .stream()
                .map(op -> new OperationItemResponse(op.getCreatedAt(), op.getOperationType(), op.getAmount(), op.getRelatedUserId()))
                .toList();
    }

    @Transactional
    public void transfer(Long fromUserId, Long toUserId, BigDecimal amount) {
        if (fromUserId.equals(toUserId)) {
            throw new BusinessException(0, "cannot transfer to same user");
        }

        Long firstId = Math.min(fromUserId, toUserId);
        Long secondId = Math.max(fromUserId, toUserId);

        Account firstLocked = accountRepository.findByUserId(firstId)
                .orElseThrow(() -> new BusinessException(0, "sender or receiver not found"));
        Account secondLocked = accountRepository.findByUserId(secondId)
                .orElseThrow(() -> new BusinessException(0, "sender or receiver not found"));

        Account from = fromUserId.equals(firstLocked.getUserId()) ? firstLocked : secondLocked;
        Account to = toUserId.equals(firstLocked.getUserId()) ? firstLocked : secondLocked;

        if (from.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(0, "insufficient funds");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        accountRepository.save(from);
        accountRepository.save(to);

        saveOperation(fromUserId, OperationType.TRANSFER_OUT, amount, toUserId);
        saveOperation(toUserId, OperationType.TRANSFER_IN, amount, fromUserId);
    }

    private void saveOperation(Long userId, OperationType type, BigDecimal amount, Long relatedUserId) {
        Operation operation = new Operation();
        operation.setUserId(userId);
        operation.setOperationType(type);
        operation.setAmount(amount);
        operation.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        operation.setRelatedUserId(relatedUserId);
        operationRepository.save(operation);
    }
}
