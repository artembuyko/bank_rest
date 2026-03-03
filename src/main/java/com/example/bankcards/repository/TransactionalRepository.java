package com.example.bankcards.repository;

import com.example.bankcards.entity.TransactionalCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionalRepository extends JpaRepository<TransactionalCard,Long> {
    @Query("SELECT t FROM TransactionalCard t WHERE t.sourceCard.id = :cardId OR t.targetCard.id = :cardId")
    Page<TransactionalCard> findAllByCard(@Param("cardId") Long cardId, Pageable pageable);
}
