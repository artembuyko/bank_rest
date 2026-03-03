package com.example.bankcards.service;

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
import com.example.bankcards.entity.enums.StatusTransaction;
import com.example.bankcards.exception.CardExceptions.*;
import com.example.bankcards.exception.TransactionalException.InsufficientFundsException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransactionalRepository;
import com.example.bankcards.security.CustomUserDetails;
import com.example.bankcards.service.Impl.CardServiceImpl;
import com.example.bankcards.service.model.UserService;
import com.example.bankcards.util.EncryptionService;
import com.example.bankcards.util.GeneratorCardNumber;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CardServiceImplTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private TransactionalRepository transactionRepository;
    @Mock
    private CardMapper cardMapper;
    @Mock
    private TransactionalMapper transactionalMapper;
    @Mock
    private UserService userService;
    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private CardServiceImpl cardService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User createUser(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private Card createCard(Long id, User owner, BigDecimal balance, StatusCard status, LocalDateTime expiry) {
        Card card = new Card();
        card.setId(id);
        card.setCardNumber("encrypted" + id);
        card.setBalance(balance);
        card.setStatus(status);
        card.setOwner(owner);
        card.setOwnerName(owner.getName());
        card.setExpiredDate(expiry);
        return card;
    }

    private Card createActiveCard(Long id, User owner, BigDecimal balance) {
        return createCard(id, owner, balance, StatusCard.ACTIVE, LocalDateTime.now().plusYears(1));
    }

    private Card createBlockedCard(Long id, User owner, BigDecimal balance) {
        return createCard(id, owner, balance, StatusCard.BLOCKED, LocalDateTime.now().plusYears(1));
    }

    private Card createExpiredCard(Long id, User owner, BigDecimal balance) {
        return createCard(id, owner, balance, StatusCard.ACTIVE, LocalDateTime.now().minusDays(1));
    }

    private TransferRequest createTransferRequest(Long fromId, Long toId, BigDecimal amount) {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(fromId);
        request.setToCardId(toId);
        request.setAmount(amount);
        return request;
    }

    private TransactionalCard createTransaction(Long id, Card source, Card target, BigDecimal amount, StatusTransaction status) {
        TransactionalCard transaction = new TransactionalCard();
        transaction.setId(id);
        transaction.setSourceCard(source);
        transaction.setTargetCard(target);
        transaction.setAmount(amount);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setStatus(status);
        return transaction;
    }

    private void authenticateAs(Long userId, String username) {
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(userId);
        //when(userDetails.getUsername()).thenReturn(username);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createCard_ShouldSucceed() {
        Long userId = 1L;
        User owner = createUser(userId, "David Stethom");
        CreateCardRequest request = new CreateCardRequest();
        request.setUserId(userId);
        request.setExpirationDate(LocalDateTime.now().plusMinutes(10));
        request.setBalance(BigDecimal.valueOf(100));

        String plainNumber = "1234567890123456";
        String encryptedNumber = "encrypted123";

        Card cardBeforeSave = new Card(); // будет заполнен маппером
        Card savedCard = createActiveCard(100L, owner, request.getBalance());
        savedCard.setCardNumber(encryptedNumber);
        CardResponse expectedResponse = new CardResponse();
        expectedResponse.setId(100L);

        when(userService.getUserById(userId)).thenReturn(owner);

        try (MockedStatic<GeneratorCardNumber> generatorMock = mockStatic(GeneratorCardNumber.class)) {
            generatorMock.when(GeneratorCardNumber::generate).thenReturn(plainNumber);

            when(encryptionService.encrypt(plainNumber)).thenReturn(encryptedNumber);
            when(cardRepository.existsByCardNumber(encryptedNumber)).thenReturn(false);
            when(cardMapper.toCard(request)).thenReturn(cardBeforeSave);
            when(cardRepository.save(any(Card.class))).thenReturn(savedCard);
            when(cardMapper.toResponse(savedCard)).thenReturn(expectedResponse);

            CardResponse result = cardService.createCard(request);

            assertThat(result).isEqualTo(expectedResponse);

            verify(cardRepository).save(argThat(c ->
                    c.getStatus() == StatusCard.ACTIVE &&
                            c.getCardNumber().equals(encryptedNumber) &&
                            c.getOwner().equals(owner) &&
                            c.getOwnerName().equals(owner.getName())
            ));
        }
    }

    @Test
    void createCard_ShouldThrowException_WhenDuplicateCardNumber() {
        Long userId = 1L;
        User owner = createUser(userId, "David Stethom");
        CreateCardRequest request = new CreateCardRequest();
        request.setUserId(userId);

        String plainNumber = "1234567890123456";
        String encryptedNumber = "encrypted123";

        when(userService.getUserById(userId)).thenReturn(owner);
        try (MockedStatic<GeneratorCardNumber> generatorMock = mockStatic(GeneratorCardNumber.class)) {
            generatorMock.when(GeneratorCardNumber::generate).thenReturn(plainNumber);
            when(encryptionService.encrypt(plainNumber)).thenReturn(encryptedNumber);
            when(cardRepository.existsByCardNumber(encryptedNumber)).thenReturn(true);

            assertThatThrownBy(() -> cardService.createCard(request))
                    .isInstanceOf(DuplicateCardNumberException.class)
                    .hasMessage("Card with this number already exists");

            verify(cardRepository, never()).save(any());
        }
    }

    @Test
    void getAllCards_ShouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Card card1 = new Card();
        Card card2 = new Card();
        Page<Card> cardPage = new PageImpl<>(List.of(card1, card2));
        CardResponse response1 = new CardResponse();
        CardResponse response2 = new CardResponse();

        when(cardRepository.findAll(pageable)).thenReturn(cardPage);
        when(cardMapper.toResponse(card1)).thenReturn(response1);
        when(cardMapper.toResponse(card2)).thenReturn(response2);

        Page<CardResponse> result = cardService.getAllCards(pageable);

        assertThat(result.getContent()).containsExactly(response1, response2);
        verify(cardRepository).findAll(pageable);
        verify(cardMapper, times(2)).toResponse(any(Card.class));
    }

    @Test
    void blockCard_ShouldSucceed_WhenCardActive() {
        Long cardId = 1L;
        Card card = createActiveCard(cardId, createUser(1L, "owner"), BigDecimal.valueOf(500));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardService.blockCard(cardId);

        assertThat(card.getStatus()).isEqualTo(StatusCard.BLOCKED);
        verify(cardRepository).save(card);
    }

    @Test
    void blockCard_ShouldThrowException_WhenCardAlreadyBlocked() {
        Long cardId = 1L;
        Card card = createBlockedCard(cardId, createUser(1L, "owner"), BigDecimal.valueOf(500));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.blockCard(cardId))
                .isInstanceOf(CardHaveBlockStatusException.class)
                .hasMessage("Card is already blocked");

        verify(cardRepository, never()).save(any());
    }

    @Test
    void blockCard_ShouldThrowException_WhenCardNotFound() {
        Long cardId = 1L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.blockCard(cardId))
                .isInstanceOf(CardNoFoundException.class)
                .hasMessage("Card not found with id: " + cardId);
    }


    @Test
    void activateCard_ShouldSucceed_WhenCardBlocked() {
        Long cardId = 1L;
        Card card = createBlockedCard(cardId, createUser(1L, "owner"), BigDecimal.valueOf(500));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardService.activateCard(cardId);

        assertThat(card.getStatus()).isEqualTo(StatusCard.ACTIVE);
        verify(cardRepository).save(card);
    }

    @Test
    void activateCard_ShouldThrowException_WhenCardAlreadyActive() {
        Long cardId = 1L;
        Card card = createActiveCard(cardId, createUser(1L, "owner"), BigDecimal.valueOf(500));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.activateCard(cardId))
                .isInstanceOf(CardHaveActiveStatusException.class)
                .hasMessage("Card is already active");

        verify(cardRepository, never()).save(any());
    }

    @Test
    void activateCard_ShouldThrowException_WhenCardNotFound() {
        Long cardId = 1L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.activateCard(cardId))
                .isInstanceOf(CardNoFoundException.class)
                .hasMessage("Card not found with id: " + cardId);
    }


    @Test
    void deleteCard_ShouldSetStatusDeleted_WhenCardExists() {
        Long cardId = 1L;
        Card card = createActiveCard(cardId, createUser(1L, "owner"), BigDecimal.valueOf(500));
        when(cardRepository.findById(cardId)).thenReturn(Optional.of(card));

        cardService.deleteCard(cardId);

        assertThat(card.getStatus()).isEqualTo(StatusCard.DELETED);
        verify(cardRepository).save(card);
    }

    @Test
    void deleteCard_ShouldThrowException_WhenCardNotFound() {
        Long cardId = 1L;
        when(cardRepository.findById(cardId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.deleteCard(cardId))
                .isInstanceOf(CardNoFoundException.class)
                .hasMessage("Card not found with id: " + cardId);
    }


    @Test
    void getAllByOwner_ShouldReturnCardsForCurrentUser() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        Pageable pageable = PageRequest.of(0, 10);

        Card card1 = new Card();
        Card card2 = new Card();
        Page<Card> cardPage = new PageImpl<>(List.of(card1, card2));
        CardResponse response1 = new CardResponse();
        CardResponse response2 = new CardResponse();

        when(cardRepository.findAllByOwner(any(Specification.class), eq(pageable))).thenReturn(cardPage);
        when(cardMapper.toResponse(card1)).thenReturn(response1);
        when(cardMapper.toResponse(card2)).thenReturn(response2);

        Page<CardResponse> result = cardService.getAllByOwner(pageable);

        assertThat(result.getContent()).containsExactly(response1, response2);

        ArgumentCaptor<Specification<Card>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(cardRepository).findAllByOwner(specCaptor.capture(), eq(pageable));
        assertThat(specCaptor.getValue()).isNotNull();
    }


    @Test
    void blockMyCard_ShouldSucceed_WhenCardOwnedAndActive() {
        Long currentUserId = 42L;
        Long cardId = 100L;
        authenticateAs(currentUserId, "user42");

        User owner = createUser(currentUserId, "owner");
        Card card = createActiveCard(cardId, owner, BigDecimal.valueOf(500));

        when(cardRepository.findByIdAndOwnerId(cardId, currentUserId)).thenReturn(Optional.of(card));

        cardService.blockMyCard(cardId);

        assertThat(card.getStatus()).isEqualTo(StatusCard.BLOCKED);
        verify(cardRepository).save(card);
    }

    @Test
    void blockMyCard_ShouldThrowException_WhenCardNotOwned() {
        Long currentUserId = 42L;
        Long cardId = 100L;
        authenticateAs(currentUserId, "user42");

        when(cardRepository.findByIdAndOwnerId(cardId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.blockMyCard(cardId))
                .isInstanceOf(CardNoFoundException.class)
                .hasMessage("Card not found or does not belong to current user");
    }

    @Test
    void blockMyCard_ShouldThrowException_WhenCardAlreadyBlocked() {
        Long currentUserId = 42L;
        Long cardId = 100L;
        authenticateAs(currentUserId, "user42");

        User owner = createUser(currentUserId, "owner");
        Card card = createBlockedCard(cardId, owner, BigDecimal.valueOf(500));

        when(cardRepository.findByIdAndOwnerId(cardId, currentUserId)).thenReturn(Optional.of(card));

        assertThatThrownBy(() -> cardService.blockMyCard(cardId))
                .isInstanceOf(CardHaveBlockStatusException.class)
                .hasMessage("Card is already blocked");

        verify(cardRepository, never()).save(any());
    }

    @Test
    void getCardBalance_ShouldReturnCardResponse_WhenCardOwned() {
        Long currentUserId = 42L;
        Long cardId = 100L;
        authenticateAs(currentUserId, "user42");

        User owner = createUser(currentUserId, "owner");
        Card card = createActiveCard(cardId, owner, BigDecimal.valueOf(500));
        CardResponse expectedResponse = new CardResponse();

        when(cardRepository.findByIdAndOwnerId(cardId, currentUserId)).thenReturn(Optional.of(card));
        when(cardMapper.toResponse(card)).thenReturn(expectedResponse);

        CardResponse result = cardService.getCardBalance(cardId);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void getCardBalance_ShouldThrowException_WhenCardNotOwned() {
        Long currentUserId = 42L;
        Long cardId = 100L;
        authenticateAs(currentUserId, "user42");

        when(cardRepository.findByIdAndOwnerId(cardId, currentUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardBalance(cardId))
                .isInstanceOf(CardNoFoundException.class)
                .hasMessage("Card not found or does not belong to current user");
    }


    @Test
    void transferAmount_ShouldSucceed() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");

        Long fromId = 1L, toId = 2L;
        BigDecimal amount = BigDecimal.valueOf(100);
        TransferRequest request = createTransferRequest(fromId, toId, amount);

        Card source = createActiveCard(fromId, owner, BigDecimal.valueOf(500));
        Card target = createActiveCard(toId, owner, BigDecimal.valueOf(200));

        TransactionalCard transactionEntity = createTransaction(999L, source, target, amount, StatusTransaction.SUCCESS);
        TransferResponse expectedResponse = new TransferResponse();
        expectedResponse.setId(999L);
        expectedResponse.setSourceCardId(fromId);
        expectedResponse.setTargetCardId(toId);
        expectedResponse.setAmount(amount);
        expectedResponse.setTimestamp(transactionEntity.getTimestamp());
        expectedResponse.setStatus("SUCCESS");

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(target));
        when(transactionalMapper.toTransaction(source, target, request)).thenReturn(transactionEntity);
        when(transactionRepository.save(transactionEntity)).thenReturn(transactionEntity);
        when(transactionalMapper.toTransferResponse(transactionEntity)).thenReturn(expectedResponse);

        TransferResponse result = cardService.transferAmount(request);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(source.getBalance()).isEqualTo(BigDecimal.valueOf(400));
        assertThat(target.getBalance()).isEqualTo(BigDecimal.valueOf(300));
        verify(cardRepository).save(source);
        verify(cardRepository).save(target);
        verify(transactionRepository).save(transactionEntity);
        verify(transactionalMapper).toTransferResponse(transactionEntity);
    }

    @Test
    void transferAmount_ShouldThrowException_WhenInsufficientFunds() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");

        Long fromId = 1L, toId = 2L;
        BigDecimal amount = BigDecimal.valueOf(600);
        TransferRequest request = createTransferRequest(fromId, toId, amount);

        Card source = createActiveCard(fromId, owner, BigDecimal.valueOf(500));
        Card target = createActiveCard(toId, owner, BigDecimal.valueOf(200));

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> cardService.transferAmount(request))
                .isInstanceOf(InsufficientFundsException.class)
                .hasMessageContaining("Insufficient funds");

        verify(cardRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
        verify(transactionalMapper, never()).toTransferResponse(any());
    }

    @Test
    void transferAmount_ShouldThrowException_WhenSourceCardNotActive() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");

        Long fromId = 1L, toId = 2L;
        BigDecimal amount = BigDecimal.valueOf(100);
        TransferRequest request = createTransferRequest(fromId, toId, amount);

        Card source = createBlockedCard(fromId, owner, BigDecimal.valueOf(500));
        Card target = createActiveCard(toId, owner, BigDecimal.valueOf(200));

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> cardService.transferAmount(request))
                .isInstanceOf(CardNotActiveException.class)
                .hasMessageContaining("Card is not active");
    }

    @Test
    void transferAmount_ShouldThrowException_WhenTargetCardNotActive() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");

        Long fromId = 1L, toId = 2L;
        BigDecimal amount = BigDecimal.valueOf(100);
        TransferRequest request = createTransferRequest(fromId, toId, amount);

        Card source = createActiveCard(fromId, owner, BigDecimal.valueOf(500));
        Card target = createBlockedCard(toId, owner, BigDecimal.valueOf(200));

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> cardService.transferAmount(request))
                .isInstanceOf(CardNotActiveException.class)
                .hasMessageContaining("Card is not active");
    }

    @Test
    void transferAmount_ShouldThrowException_WhenSourceCardExpired() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");

        Long fromId = 1L, toId = 2L;
        BigDecimal amount = BigDecimal.valueOf(100);
        TransferRequest request = createTransferRequest(fromId, toId, amount);

        Card source = createExpiredCard(fromId, owner, BigDecimal.valueOf(500));
        Card target = createActiveCard(toId, owner, BigDecimal.valueOf(200));

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> cardService.transferAmount(request))
                .isInstanceOf(CardExpiredException.class)
                .hasMessageContaining("Card has expired");

        assertThat(source.getStatus()).isEqualTo(StatusCard.EXPIRED);
        verify(cardRepository).save(source);
    }

    @Test
    void transferAmount_ShouldThrowException_WhenTargetCardExpired() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");

        Long fromId = 1L, toId = 2L;
        BigDecimal amount = BigDecimal.valueOf(100);
        TransferRequest request = createTransferRequest(fromId, toId, amount);

        Card source = createActiveCard(fromId, owner, BigDecimal.valueOf(500));
        Card target = createExpiredCard(toId, owner, BigDecimal.valueOf(200));

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> cardService.transferAmount(request))
                .isInstanceOf(CardExpiredException.class)
                .hasMessageContaining("Card has expired");

        assertThat(target.getStatus()).isEqualTo(StatusCard.EXPIRED);
        verify(cardRepository).save(target);
    }

    @Test
    void transferAmount_ShouldThrowException_WhenSourceCardNotOwnedByCurrentUser() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");
        User otherUser = createUser(99L, "other");

        Long fromId = 1L, toId = 2L;
        BigDecimal amount = BigDecimal.valueOf(100);
        TransferRequest request = createTransferRequest(fromId, toId, amount);

        Card source = createActiveCard(fromId, otherUser, BigDecimal.valueOf(500));
        Card target = createActiveCard(toId, owner, BigDecimal.valueOf(200));

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> cardService.transferAmount(request))
                .isInstanceOf(CardNotOwnedException.class)
                .hasMessageContaining("Card does not belong to current user");
    }

    @Test
    void transferAmount_ShouldThrowException_WhenTargetCardNotOwnedByCurrentUser() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");
        User otherUser = createUser(99L, "other");

        Long fromId = 1L, toId = 2L;
        BigDecimal amount = BigDecimal.valueOf(100);
        TransferRequest request = createTransferRequest(fromId, toId, amount);

        Card source = createActiveCard(fromId, owner, BigDecimal.valueOf(500));
        Card target = createActiveCard(toId, otherUser, BigDecimal.valueOf(200));

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> cardService.transferAmount(request))
                .isInstanceOf(CardNotOwnedException.class)
                .hasMessageContaining("Card does not belong to current user");
    }

    @Test
    void transferAmount_ShouldThrowException_WhenTransferToSameCard() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");

        Long fromId = 1L;
        BigDecimal amount = BigDecimal.valueOf(100);
        TransferRequest request = createTransferRequest(fromId, fromId, amount);

        Card source = createActiveCard(fromId, owner, BigDecimal.valueOf(500));

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));

        assertThatThrownBy(() -> cardService.transferAmount(request))
                .isInstanceOf(SameCardTransferException.class)
                .hasMessageContaining("Cannot transfer to the same card");
    }

    @Test
    void transferAmount_ShouldThrowException_WhenAmountIsZeroOrNegative() {
        Long currentUserId = 42L;
        authenticateAs(currentUserId, "user42");
        User owner = createUser(currentUserId, "owner");

        Long fromId = 1L, toId = 2L;
        BigDecimal amount = BigDecimal.ZERO;
        TransferRequest request = createTransferRequest(fromId, toId, amount);

        Card source = createActiveCard(fromId, owner, BigDecimal.valueOf(500));
        Card target = createActiveCard(toId, owner, BigDecimal.valueOf(200));

        when(userService.getUserById(currentUserId)).thenReturn(owner);
        when(cardRepository.findById(fromId)).thenReturn(Optional.of(source));
        when(cardRepository.findById(toId)).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> cardService.transferAmount(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Transfer amount must be positive");
    }
}