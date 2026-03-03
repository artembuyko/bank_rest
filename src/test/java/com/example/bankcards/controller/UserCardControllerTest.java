
package com.example.bankcards.controller;

import com.example.bankcards.Config.TestSecurityConfig;
import com.example.bankcards.dto.Requests.TransferRequest;
import com.example.bankcards.dto.Response.CardResponse;
import com.example.bankcards.dto.Response.TransferResponse;
import com.example.bankcards.security.CustomUserDetailsService;
import com.example.bankcards.security.JwtTokenProvider;
import com.example.bankcards.service.model.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserCardController.class)
@WithMockUser
@Import(TestSecurityConfig.class)
class UserCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CardService cardService;

    @Test
    void getMyCard_ShouldReturnPageOfCards() throws Exception {
        Pageable pageable = PageRequest.of(0, 20);
        CardResponse card1 = new CardResponse();
        card1.setId(1L);
        CardResponse card2 = new CardResponse();
        card2.setId(2L);
        Page<CardResponse> page = new PageImpl<>(List.of(card1, card2), pageable, 2);

        when(cardService.getAllByOwner(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/card/user/my")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2));

        verify(cardService).getAllByOwner(any(Pageable.class));
    }

    @Test
    void blockMyCard_ShouldReturnOk() throws Exception {
        Long cardId = 1L;

        mockMvc.perform(put("/api/v1/card/user/{cardId}/block", cardId))
                .andExpect(status().isOk());

        verify(cardService).blockMyCard(cardId);
    }

    @Test
    void getCardBalance_ShouldReturnCardResponse() throws Exception {
        Long cardId = 1L;
        CardResponse response = new CardResponse();
        response.setId(cardId);
        response.setBalance(BigDecimal.valueOf(500));

        when(cardService.getCardBalance(cardId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/card/user/{cardId}/balance", cardId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.balance").value(500));

        verify(cardService).getCardBalance(cardId);
    }

    @Test
    void transfer_ShouldReturnTransferResponse() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setFromCardId(1L);
        request.setToCardId(2L);
        request.setAmount(BigDecimal.valueOf(100));

        TransferResponse response = new TransferResponse();
        response.setId(100L);
        response.setSourceCardId(1L);
        response.setTargetCardId(2L);
        response.setAmount(BigDecimal.valueOf(100));
        response.setTimestamp(LocalDateTime.now());
        response.setStatus("SUCCESS");

        when(cardService.transferAmount(any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/card/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.sourceCardId").value(1L))
                .andExpect(jsonPath("$.targetCardId").value(2L))
                .andExpect(jsonPath("$.amount").value(100))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(cardService).transferAmount(any(TransferRequest.class));
    }

    @Test
    void transfer_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        TransferRequest invalidRequest = new TransferRequest();

        mockMvc.perform(post("/api/v1/card/user/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}