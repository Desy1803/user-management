package com.assignment.user_management.model;

import jakarta.annotation.Nullable;
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
public class ModifyUserRequest {

    @Nullable
    @Size(min = 2, max = 50, message = "Il nome deve contenere tra 2 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Il nome deve contenere solo lettere")
    private String firstName;

    @Nullable
    @Size(min = 2, max = 50, message = "Il cognome deve contenere tra 2 e 50 caratteri")
    @Pattern(regexp = "^[a-zA-Z]+$", message = "Il cognome deve contenere solo lettere")
    private String lastName;

    @Nullable
    @Email(message = "L'email deve essere valida")
    private String email;

    @Nullable
    @Size(min = 5, max = 100, message = "L'indirizzo deve contenere tra 5 e 100 caratteri")
    private String address;
}
