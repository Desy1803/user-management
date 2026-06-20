package com.assignment.user_management.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record ModifyUserRequest(
  
    @Size(min = 2, max = 50, message = "Il nome deve contenere tra 2 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Il nome deve contenere solo lettere")
    String firstName,

    @Size(min = 2, max = 50, message = "Il cognome deve contenere tra 2 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Il cognome deve contenere solo lettere")
    String lastName,

    @Email(message = "L'email deve essere valida")
    String email,

    @Size(min = 5, max = 100, message = "L'indirizzo deve contenere tra 5 e 100 caratteri")
    String address
) {}