package com.assignment.user_management.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UserRequest(
    @NotBlank(message = "Il nome è obbligatorio")
    @NotNull(message = "Il nome non può essere null")
    @Size(min = 2, max = 50, message = "Il nome deve contenere tra 2 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Il nome deve contenere solo lettere")
    String firstName,

    @NotBlank(message = "Il cognome è obbligatorio")
    @NotNull(message = "Il cognome non può essere null")
    @Size(min = 2, max = 50, message = "Il cognome deve contenere tra 2 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Il cognome deve contenere solo lettere")
    String lastName,

    @NotBlank(message = "L'email è obbligatoria")
    @NotNull(message = "L'email non può essere null")
    @Email(message = "L'email deve essere valida")
    String email,

    @NotBlank(message = "L'indirizzo è obbligatorio")
    @NotNull(message = "L'indirizzo non può essere null")
    @Size(min = 5, max = 100, message = "L'indirizzo deve contenere tra 5 e 100 caratteri")
    String address
) {}