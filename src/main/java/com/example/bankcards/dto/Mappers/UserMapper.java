package com.example.bankcards.dto.Mappers;

import com.example.bankcards.dto.Requests.CreateUserRequest;
import com.example.bankcards.dto.Response.UserResponse;
import com.example.bankcards.entity.User;
import com.example.bankcards.entity.enums.Role;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "role", expression = "java(stringToRole(request.getRole()))")
    User toUser(CreateUserRequest request);


    @Mapping(target = "id",source = "id")
    @Mapping(target = "username",source = "username")
    @Mapping(target = "role",source = "role")
    @Mapping(target = "fullName",source = "name")
    @Mapping(target = "status",source = "status")
    UserResponse toResponse(User user);

    default Role stringToRole(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return Role.USER;
        }
        String formatted = roleStr.toUpperCase();
        if (formatted.startsWith("ROLE_")) {
            formatted = formatted.substring(5);
        }
        try {
            return Role.valueOf(formatted);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Недопустимая роль: " + roleStr);
        }
    }

}
