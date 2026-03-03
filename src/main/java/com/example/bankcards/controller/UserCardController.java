package com.example.bankcards.controller;

import com.example.bankcards.dto.Requests.TransferRequest;
import com.example.bankcards.dto.Response.CardResponse;
import com.example.bankcards.dto.Response.TransferResponse;
import com.example.bankcards.service.model.CardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api/v1/card/user")
@PreAuthorize("isAuthenticated")
public class UserCardController {

    private final CardService cardService;

    @GetMapping("/my")
    public ResponseEntity<Page<CardResponse>> getMyCard(@PageableDefault(size = 20,sort = "id",direction = Sort.Direction.ASC)Pageable pageable){
        Page<CardResponse> response = cardService.getAllByOwner(pageable);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/{cardId}/block")
    public ResponseEntity<Void> blockMyCard(@PathVariable Long cardId) {
        cardService.blockMyCard(cardId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{cardId}/balance")
    public ResponseEntity<CardResponse> getCardBalance(@PathVariable Long cardId) {
        CardResponse response = cardService.getCardBalance(cardId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(@Valid @RequestBody TransferRequest request) {
        TransferResponse response = cardService.transferAmount(request);
        return ResponseEntity.ok(response);
    }
}
