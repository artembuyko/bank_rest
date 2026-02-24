package com.example.bankcards.repository;

import com.example.bankcards.entity.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionalRepository extends JpaRepository<Transactional,Long> {

    @Query("SELECT t FROM Transaction t WHERE t.sourceCard_id = :card.id OR t.currentCard=:card.id")
    Page<Transactional> findAllByCard(Long cardId,Pageable pageable);
}
