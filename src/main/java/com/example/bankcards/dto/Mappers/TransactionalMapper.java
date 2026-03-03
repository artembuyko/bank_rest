package com.example.bankcards.dto.Mappers;

import com.example.bankcards.dto.Requests.TransferRequest;
import com.example.bankcards.dto.Response.TransferResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.TransactionalCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransactionalMapper{

    @Mapping(source = "sourceCard", target = "sourceCard")
    @Mapping(source = "targetCard", target = "targetCard")
    @Mapping(source = "request.amount", target = "amount")
    @Mapping(target = "id",ignore = true)
    @Mapping(target = "createdAt",ignore = true)
    @Mapping(target = "updatedAt",ignore = true)
    @Mapping(target = "status", ignore = true)
    TransactionalCard toTransaction(Card sourceCard, Card targetCard, TransferRequest request);

    @Mapping(source = "sourceCard.id", target = "sourceCardId")
    @Mapping(source = "targetCard.id", target = "targetCardId")
    TransferResponse toTransferResponse(TransactionalCard transactional);
}
