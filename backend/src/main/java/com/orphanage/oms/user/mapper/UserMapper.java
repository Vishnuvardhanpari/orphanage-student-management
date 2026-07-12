package com.orphanage.oms.user.mapper;

import com.orphanage.oms.user.dto.UserDetailResponse;
import com.orphanage.oms.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Maps {@link User} entities to management response DTOs.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "role", source = "role.name")
    @Mapping(
            target = "accountNonLocked",
            expression = "java(user.isAccountNonLocked() && !user.isCurrentlyLocked())")
    UserDetailResponse toDetailResponse(User user);
}
