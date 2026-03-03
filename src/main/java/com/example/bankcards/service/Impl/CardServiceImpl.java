package com.example.bankcards.service.Impl;

import com.example.bankcards.dto.Mappers.CardMapper;
import com.example.bankcards.dto.Mappers.TransactionalMapper;
import com.example.bankcards.dto.Requests.CreateCardRequest;
import com.example.bankcards.dto.Requests.TransferRequest;
import com.example.bankcards.dto.Response.CardResponse;
import com.example.bankcards.dto.Response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.TransactionalCard;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.StatusCard;
import com.example.bankcards.exception.CardExceptions.*;
import com.example.bankcards.exception.TransactionalException.InsufficientFundsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionalRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.model.CardService;
import com.example.bankcards.service.model.UserService;
import com.example.bankcards.util.EncryptionService;
import com.example.bankcards.util.GeneratorCardNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CardServiceImpl implements CardService {

    private final CardRepository cardRepository;
    private final TransactionalRepository transactionRepository;
    private final CardMapper mapper;
    private final TransactionalMapper transactionalMapper;
    private final UserService userService;
    private final EncryptionService encryptionService;

    @Override
    @Transactional
    public CardResponse createCard(CreateCardRequest request) {
        log.info("Creating new card for user ID: {}", request.getUserId());

        User owner = userService.getUserById(request.getUserId());

        String cardNumber = GeneratorCardNumber.generate();
        String encryptedNumber = encryptionService.encrypt(cardNumber);

        if (cardRepository.existsByCardNumber(encryptedNumber)) {
            throw new DuplicateCardNumberException("Card with this number already exists");
        }

        Card card = mapper.toCard(request);
        card.setOwner(owner);
        card.setOwnerName(owner.getName());
        card.setStatus(StatusCard.ACTIVE);
        card.setCardNumber(encryptedNumber);
        Card savedCard = cardRepository.save(card);
        log.info("Card created with ID: {}", savedCard.getId());

        return mapper.toResponse(savedCard);
    }

    @Override
    public Page<CardResponse> getAllCards(Pageable pageable) {
        return cardRepository.findAll(pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void blockCard(Long cardId) {
        log.info("Blocking card ID: {} by admin", cardId);
        Card card = getCardEntityOrThrow(cardId);
        if (card.getStatus() == StatusCard.BLOCKED) {
            throw new CardHaveBlockStatusException("Card is already blocked");
        }
        card.setStatus(StatusCard.BLOCKED);
        cardRepository.save(card);
        log.info("Card {} blocked by admin", cardId);
    }

    @Override
    @Transactional
    public void activateCard(Long cardId) {
        log.info("Activating card ID: {} by admin", cardId);
        Card card = getCardEntityOrThrow(cardId);
        if (card.getStatus() == StatusCard.ACTIVE) {
            throw new CardHaveActiveStatusException("Card is already active");
        }
        card.setStatus(StatusCard.ACTIVE);
        cardRepository.save(card);
        log.info("Card {} activated by admin", cardId);
    }

    @Override
    @Transactional
    public void deleteCard(Long id) {
        log.info("Soft deleting card ID: {} by admin", id);
        Card card = getCardEntityOrThrow(id);
        card.setStatus(StatusCard.DELETED);
        cardRepository.save(card);
        log.info("Card {} marked as deleted", id);
    }

    @Override
    public Page<CardResponse> getAllByOwner(Pageable pageable) {
        Long currentUserId = getCurrentUserId();
        log.info("Requesting cards for user ID: {}", currentUserId);
        Specification<Card> spec = byUserId(currentUserId);
        Page<Card> cards = cardRepository.findAllByOwner(spec, pageable);
        return cards.map(mapper::toResponse);
    }

    @Override
    @Transactional
    public void blockMyCard(Long cardId) {
        Long currentUserId = getCurrentUserId();
        log.info("User {} requests blocking card {}", currentUserId, cardId);
        Card card = findCardByIdAndUser(cardId, currentUserId);
        if (card.getStatus() == StatusCard.BLOCKED) {
            throw new CardHaveBlockStatusException("Card is already blocked");
        }
        card.setStatus(StatusCard.BLOCKED);
        cardRepository.save(card);
        log.info("Card {} blocked by owner", cardId);
    }

    @Override
    public CardResponse getCardBalance(Long cardId) {
        Long currentUserId = getCurrentUserId();
        log.info("Balance request for card {} by user {}", cardId, currentUserId);
        Card card = findCardByIdAndUser(cardId, currentUserId);
        return mapper.toResponse(card);
    }

    @Override
    @Transactional
    public TransferResponse transferAmount(TransferRequest request) {
        log.info("Transfer request: from card {} to card {}, amount {}",
                request.getFromCardId(), request.getToCardId(), request.getAmount());

        User owner = getCurrentUser();

        Card sourceCard = getCardEntityOrThrow(request.getFromCardId());
        Card targetCard = getCardEntityOrThrow(request.getToCardId());

        validateTransfer(sourceCard, targetCard, owner.getId());

        if (sourceCard.getBalance().compareTo(request.getAmount()) < 0) {
            log.warn("Transfer declined: insufficient funds on card {}", sourceCard.getId());
            throw new InsufficientFundsException("Insufficient funds on source card");
        }

        if (request.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }

        sourceCard.setBalance(sourceCard.getBalance().subtract(request.getAmount()));
        targetCard.setBalance(targetCard.getBalance().add(request.getAmount()));

        cardRepository.save(sourceCard);
        cardRepository.save(targetCard);

        TransactionalCard transaction = transactionalMapper.toTransaction(sourceCard, targetCard, request);
        transactionRepository.save(transaction);

        log.info("Transfer completed successfully, transaction ID: {}", transaction.getId());
        return transactionalMapper.toTransferResponse(transaction);
    }

    private Card findCardByIdAndUser(Long cardId, Long userId) {
        return cardRepository.findByIdAndOwnerId(cardId, userId)
                .orElseThrow(() -> new CardNoFoundException("Card not found or does not belong to current user"));
    }

    private void validateTransfer(Card from, Card to, Long ownerId) {
        validateCardOwnership(from, ownerId);
        validateCardOwnership(to, ownerId);

        validateCardActive(from);
        validateCardActive(to);

        validateCardExpiry(from);
        validateCardExpiry(to);

        if (from.getId().equals(to.getId())) {
            throw new SameCardTransferException("Cannot transfer to the same card");
        }
    }

    private void validateCardOwnership(Card card, Long ownerId) {
        if (!card.getOwner().getId().equals(ownerId)) {
            log.info("Card with ID {} does not belong to user {}", card.getId(), ownerId);
            throw new CardNotOwnedException("Card does not belong to current user");
        }
    }

    private void validateCardActive(Card card) {
        if (card.getStatus() != StatusCard.ACTIVE) {
            log.info("Card {} is not active, status: {}", card.getId(), card.getStatus());
            throw new CardNotActiveException("Card is not active");
        }
    }

    private void validateCardExpiry(Card card) {
        if (card.getExpiredDate().toLocalDate().isBefore(java.time.LocalDate.now())) {
            log.info("Card {} has expired: {}", card.getId(), card.getExpiredDate());
            card.setStatus(StatusCard.EXPIRED);
            cardRepository.save(card);
            throw new CardExpiredException("Card has expired");
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        } else if (principal instanceof String) {
            return userService.findByUsername((String) principal).getId();
        }
        throw new SecurityException("Unsupported principal type");
    }

    private User getCurrentUser() {
        Long userId = getCurrentUserId();
        return userService.getUserById(userId);
    }

    private Card getCardEntityOrThrow(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNoFoundException("Card not found with id: " + id));
    }

    private Specification<Card> byUserId(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), userId);
    }
}