package com.assignment.user_management.controller;

import com.assignment.user_management.model.UserResponse;
import com.assignment.user_management.model.ModifyUserRequest;
import com.assignment.user_management.model.UserRequest;
import com.assignment.user_management.service.UserService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "API per la gestione degli utenti")
@OpenAPIDefinition(
    info = @Info(
        title = "User Management API",
        version = "1.0.0",
        description = "API REST per CRUD degli utenti, ricerca avanzata e import CSV"
    )
)
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Ricerca e lista utenti", description = "Recupera tutti gli utenti con filtri opzionali per nome e cognome")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista utenti recuperata con successo")
    })
    public Page<UserResponse> getUsers(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(defaultValue = "0") @Min(0)int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        logger.info("GET /api/users - ricerca utenti con page: {}, size: {}", page, size);
        return userService.searchUsers(firstName, lastName, size, page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ottieni utente per ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utente trovato"),
        @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    public UserResponse getUser(@PathVariable Long id) {
        logger.info("GET /api/users/{} - recupero utente per id", id);
        return userService.getUserById(id);
    }

    @PostMapping
    @Operation(summary = "Crea nuovo utente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Utente creato con successo"),
        @ApiResponse(responseCode = "400", description = "Dati utente non validi")
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserRequest userRequest) {
        logger.info("POST /api/users - creazione nuovo utente: {} {}", userRequest.firstName(), userRequest.lastName());
        UserResponse created = userService.createUser(userRequest);
        logger.info("Utente creato con id: {}", created.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Aggiorna utente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utente aggiornato con successo"),
        @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    public UserResponse updateUser(@PathVariable Long id, @Valid @RequestBody ModifyUserRequest userRequest) {
        // Aggiorna solo i campi forniti nell'oggetto di modifica.
        logger.info("PUT /api/users/{} - aggiornamento utente", id);
        return userService.updateUser(id, userRequest);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Elimina utente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Utente eliminato con successo"),
        @ApiResponse(responseCode = "404", description = "Utente non trovato")
    })
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.info("DELETE /api/users/{} - eliminazione utente", id);
        userService.deleteUser(id);
        logger.info("Utente con id {} eliminato", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Importa utenti da CSV", description = "Carica un file CSV e importa gli utenti nel database")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Utenti importati con successo"),
        @ApiResponse(responseCode = "400", description = "File CSV non valido")
    })
    public ResponseEntity<List<UserResponse>> importUsers(@RequestPart("file") MultipartFile file) {
        logger.info("POST /api/users/import - importazione file CSV: {} (size: {} bytes)", file.getOriginalFilename(), file.getSize());
        List<UserResponse> importedUsers = userService.importFromCsv(file);
        logger.info("Importazione completata: {} utenti importati", importedUsers.size());
        return ResponseEntity.ok(importedUsers);
    }
}
