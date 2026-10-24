package com.jsmc.spacemap.project.mapper;

import com.jsmc.spacemap.project.dto.response.UserResponse;
import com.jsmc.spacemap.project.entity.User;

public final class UserMapper {
    private UserMapper() { }

    public static UserResponse toResponse(User user) {
        if (user == null) return null;
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }
}
