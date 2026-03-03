package com.example.bankcards.dto.Mappers;

import com.example.bankcards.dto.Requests.CreateCardRequest;
import com.example.bankcards.dto.Response.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.util.MaskingService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public abstract class CardMapper {

    @Autowired
    protected MaskingService maskingService;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cardNumber", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "ownerName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "expiredDate", source = "expirationDate")
    @Mapping(target = "balance", source = "balance")
    public abstract Card toCard(CreateCardRequest request);

    @Mapping(target = "maskNumber", expression = "java(maskCardNumber(card.getCardNumber()))")
    @Mapping(source = "expiredDate", target = "expirationDate")
    public abstract CardResponse toResponse(Card card);

    protected String maskCardNumber(String encryptedNumber) {
        return maskingService.mask(encryptedNumber);
    }
}
