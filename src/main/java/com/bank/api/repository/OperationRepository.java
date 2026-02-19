package com.bank.api.repository;

import com.bank.api.entity.Operation;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OperationRepository extends JpaRepository<Operation, Long> {

    List<Operation> findByUserIdAndCreatedAtBetweenOrderByCreatedAtDesc(Long userId, OffsetDateTime from, OffsetDateTime to);
}
