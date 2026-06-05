package com.utility.billing.auth.mapper;

import com.utility.billing.auth.dto.UserResponse;
import com.utility.billing.auth.entity.User;

import java.util.List;

public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getRoleName())
                .toList();
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .nationalId(user.getNationalId())
                .address(user.getAddress())
                .status(user.getStatus().name())
                .emailVerified(user.isEmailVerified())
                .mustChangePassword(user.isMustChangePassword())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
