package com.assignment.user_management.model;

import lombok.Builder;

@Builder
public record UserResponse(
    Long id,
    String firstName,
    String lastName,
    String email,
    String address
) {}