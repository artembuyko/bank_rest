package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card,Long>, JpaSpecificationExecutor<Card> {
    Optional<Card> findCardByCardNumber(String cardNumber);
    Page<Card> findAllByOwner(Specification<Card> spec, Pageable pageable);
    boolean existsByOwnerAndId(User owner,Long carId);
    boolean existsByCardNumber(String cardNumber);
    Optional<Card> findByIdAndOwnerId(Long cardId, Long ownerId);
}
