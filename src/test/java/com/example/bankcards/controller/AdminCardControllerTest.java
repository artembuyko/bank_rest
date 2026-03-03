package com.example.bankcards.controller;

import com.example.bankcards.Config.TestSecurityConfig;
import com.example.bankcards.dto.Requests.CreateCardRequest;
import com.example.bankcards.dto.Response.CardResponse;
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

@WebMvcTest(AdminCardController.class)
@Import(TestSecurityConfig.class)
@WithMockUser(roles = "ADMIN")
class AdminCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CardService cardService;

    @Test
    void createCard_ShouldReturnCreatedCard() throws Exception {
        // Arrange
        CreateCardRequest request = new CreateCardRequest();
        request.setUserId(1L);
        request.setExpirationDate(LocalDateTime.now().plusYears(3));
        request.setBalance(BigDecimal.valueOf(1000));

        CardResponse response = new CardResponse();
        response.setId(100L);
        response.setCardNumber(" **** **** **** 1234");
        response.setBalance(BigDecimal.valueOf(1000));

        when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100L))
                .andExpect(jsonPath("$.cardNumber").value(" **** **** **** 1234"))
                .andExpect(jsonPath("$.balance").value(1000));

        verify(cardService).createCard(any(CreateCardRequest.class));
    }

    @Test
    void createCard_WithInvalidRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        CreateCardRequest invalidRequest = new CreateCardRequest();

        // Act & Assert
        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCards_ShouldReturnPageOfCards() throws Exception {
        // Arrange
        Pageable pageable = PageRequest.of(0, 20);
        CardResponse card1 = new CardResponse();
        card1.setId(1L);
        CardResponse card2 = new CardResponse();
        card2.setId(2L);
        Page<CardResponse> page = new PageImpl<>(List.of(card1, card2), pageable, 2);

        when(cardService.getAllCards(any(Pageable.class))).thenReturn(page);

        // Act & Assert
        mockMvc.perform(get("/api/admin/cards")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[1].id").value(2));

        verify(cardService).getAllCards(any(Pageable.class));
    }

    @Test
    void blockCard_ShouldReturnOk() throws Exception {
        // Arrange
        Long cardId = 1L;

        // Act & Assert
        mockMvc.perform(put("/api/admin/cards/{cardId}/block", cardId))
                .andExpect(status().isOk());

        verify(cardService).blockCard(cardId);
    }

    @Test
    void activateCard_ShouldReturnOk() throws Exception {
        // Arrange
        Long cardId = 1L;

        // Act & Assert
        mockMvc.perform(put("/api/admin/cards/{cardId}/activate", cardId))
                .andExpect(status().isOk());

        verify(cardService).activateCard(cardId);
    }

    @Test
    void deleteCard_ShouldReturnNoContent() throws Exception {
        // Arrange
        Long cardId = 1L;

        // Act & Assert
        mockMvc.perform(delete("/api/admin/cards/{cardId}", cardId))
                .andExpect(status().isNoContent());

        verify(cardService).deleteCard(cardId);
    }

    @Test
    @WithMockUser(roles = "USER") // тест доступа: не админ
    void endpoints_ShouldReturnForbidden_WhenUserNotAdmin() throws Exception {
        mockMvc.perform(post("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/admin/cards"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/cards/1/block"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/admin/cards/1/activate"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/cards/1"))
                .andExpect(status().isForbidden());
    }
}
