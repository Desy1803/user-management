package com.assignment.user_management.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {

    @NotBlank(message = "Il nome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il nome deve contenere tra 2 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Il nome deve contenere solo lettere")
    private String firstName;

    @NotBlank(message = "Il cognome è obbligatorio")
    @Size(min = 2, max = 50, message = "Il cognome deve contenere tra 2 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Il cognome deve contenere solo lettere")
    private String lastName;

    @NotBlank(message = "L'email è obbligatoria")
    @Email(message = "L'email deve essere valida")
    private String email;

    @NotBlank(message = "L'indirizzo è obbligatorio")
    @Size(min = 5, max = 100, message = "L'indirizzo deve contenere tra 5 e 100 caratteri")
    private String address;
}
