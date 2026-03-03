package com.example.bankcards.service.model;

import com.example.bankcards.dto.Requests.CreateCardRequest;
import com.example.bankcards.dto.Requests.TransferRequest;
import com.example.bankcards.dto.Response.CardResponse;
import com.example.bankcards.dto.Response.TransferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface CardService {

        CardResponse createCard(CreateCardRequest request);
        Page<CardResponse> getAllCards(Pageable pageable);
        void blockCard(Long cardId);
        void activateCard(Long cardId);
        void deleteCard(Long id);
        Page<CardResponse> getAllByOwner(Pageable pageable);
        void blockMyCard(Long cardId);
        CardResponse getCardBalance(Long cardId);
        TransferResponse transferAmount(TransferRequest request);
}
