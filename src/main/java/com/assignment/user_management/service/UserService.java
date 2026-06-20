package com.assignment.user_management.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.assignment.user_management.entity.User;
import com.assignment.user_management.exception.CsvImportException;
import com.assignment.user_management.exception.ResourceNotFoundException;
import com.assignment.user_management.model.ModifyUserRequest;
import com.assignment.user_management.model.UserRequest;
import com.assignment.user_management.model.UserResponse;
import com.assignment.user_management.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserRepository userRepository;

    public Page<UserResponse> searchUsers(String firstName, String lastName, int size, int page) {
        logger.info("Ricerca utenti con firstName: {}, lastName: {}, page: {}, size: {}", firstName, lastName, page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage = userRepository.findByFirstNameAndLastName(firstName, lastName, pageable);
        return userPage.map(this::toResponse);
    }

    public UserResponse getUserById(Long id) {
        logger.info("Recupero utente con id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));
        return toResponse(user);
    }

    public UserResponse createUser(UserRequest user) {
        logger.info("Creazione utente: {} {}", user.firstName(), user.lastName());
        userRepository.findByEmail(user.email()).ifPresent(existing -> {
            throw new IllegalArgumentException("Email already in use: " + user.email());
        });
        User userEntity = fromRequest(user);
        User savedUser = userRepository.save(userEntity);
        return toResponse(savedUser);
    }

    public UserResponse updateUser(Long id, ModifyUserRequest updatedUser) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User with id " + id + " not found"));

        logger.info("Aggiornamento utente con id: {}", id);

        if (updatedUser.firstName() != null && !updatedUser.firstName().isBlank()) {
            user.setFirstName(updatedUser.firstName());
        }
        if (updatedUser.lastName() != null && !updatedUser.lastName().isBlank()) {
            user.setLastName(updatedUser.lastName());
        }
        if (updatedUser.email() != null && !updatedUser.email().isBlank()) {
            user.setEmail(updatedUser.email());
        }
        if (updatedUser.address() != null && !updatedUser.address().isBlank()) {
            user.setAddress(updatedUser.address());
        }

        if(userRepository.findByEmail(user.getEmail()).filter(u -> !u.getId().equals(id)).isPresent()) {
            throw new IllegalArgumentException("Email already in use: " + user.getEmail());
        }
        User updatedEntity = userRepository.save(user);
        return toResponse(updatedEntity);
    }

    public void deleteUser(Long id) {
        logger.info("Eliminazione utente con id: {}", id);
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User with id " + id + " not found");
        }
        userRepository.deleteById(id);
    }
    @Transactional
    public List<UserResponse> importFromCsv(MultipartFile file) {
        logger.info("Inizio importazione CSV da file: {}", file.getOriginalFilename());
        if (file == null || file.isEmpty()) {
            return List.of();
        }

        Pattern emailPattern = Pattern.compile(".+@.+\\..+");
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        List<CSVRecord> records;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
            CSVParser parser = csvFormat.parse(reader)) {
            records = parser.getRecords();
        } catch (IOException e) {
            logger.error("Errore durante la lettura del file CSV", e);
            throw new CsvImportException("Impossibile leggere il file CSV: " + e.getMessage());
        }

        // 1. Estrai tutte le email uniche dal CSV per fare un controllo massivo
        Set<String> emailsInCsv = records.stream()
                .map(rec -> rec.get("email"))
                .filter(email -> email != null && !email.isBlank())
                .collect(Collectors.toSet());

        Set<String> existingEmailsInDb = userRepository.findByEmailIn(emailsInCsv).stream()
        .map(userEntity -> userEntity.getEmail()) 
        .collect(Collectors.toSet());

        List<User> usersToSave = new ArrayList<>();
        Set<String> processedEmailsInCurrentBatch = new HashSet<>();

        // 3. Elabora i record usando i set in memoria (operazioni O(1))
        for (CSVRecord rec : records) {
            try {
                String firstName = rec.get("firstName");
                String lastName = rec.get("lastName");
                String email = rec.get("email");
                String address = rec.get("address");

                // Controlla se esiste già nel DB
                if (existingEmailsInDb.contains(email)) {
                    logger.warn("Riga {} scartata: email già esistente nel DB: {}", rec.getRecordNumber(), email);
                    continue;
                }

                // Controlla se ci sono duplicati ALL'INTERNO dello stesso CSV
                if (processedEmailsInCurrentBatch.contains(email)) {
                    logger.warn("Riga {} scartata: email duplicata all'interno del file CSV: {}", rec.getRecordNumber(), email);
                    continue;
                }

                if (!email.isBlank() && emailPattern.matcher(email).matches()) {
                    User user = User.builder()
                            .firstName(firstName)
                            .lastName(lastName)
                            .email(email)
                            .address(address)
                            .build();
                    usersToSave.add(user);
                    processedEmailsInCurrentBatch.add(email);
                } else {
                    logger.warn("Riga {} scartata: email vuota o non valida: {}", rec.getRecordNumber(), email);
                }
            } catch (IllegalArgumentException e) {
                throw new CsvImportException("Struttura dell'header CSV non valida.");
            }
        }

        logger.info("Salvataggio massivo di {} utenti", usersToSave.size());
        List<User> savedUsers = userRepository.saveAll(usersToSave);
        return savedUsers.stream().map(this::toResponse).toList();
    }
    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAddress()
        );
    }

    private User fromRequest(UserRequest userRequest) {
        return User.builder()
                .firstName(userRequest.firstName())
                .lastName(userRequest.lastName())
                .email(userRequest.email())
                .address(userRequest.address())
                .build();
    }
}