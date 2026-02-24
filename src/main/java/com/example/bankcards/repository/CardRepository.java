package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<Card,Long> {
    Page<Card> findAllCard(User owner, Pageable pageable);

    Optional<Card> findCardByCardNumber(String cardNumber);

    boolean existsByOwnerAndId(User owner,Long id);
}
