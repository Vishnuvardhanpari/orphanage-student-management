package com.orphanage.oms.auth.mapper;

import com.orphanage.oms.auth.dto.UserResponse;
import com.orphanage.oms.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps user entities to auth response DTOs.
 */
@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "role", source = "role.name")
    UserResponse toUserResponse(User user);
}
