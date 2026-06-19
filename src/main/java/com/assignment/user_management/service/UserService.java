package com.assignment.user_management.service;

import com.assignment.user_management.entity.User;
import com.assignment.user_management.exception.CsvImportException;
import com.assignment.user_management.exception.ResourceNotFoundException;
import com.assignment.user_management.model.ModifyUserRequest;
import com.assignment.user_management.model.UserRequest;
import com.assignment.user_management.model.UserResponse;
import com.assignment.user_management.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
                .orElseThrow(() -> {
                    logger.warn("Utente con id {} non trovato", id);
                    return new ResourceNotFoundException("User with id " + id + " not found");
                });
        logger.debug("Utente trovato: {}", user.getId());
        return toResponse(user);
    }

    public UserResponse createUser(UserRequest user) {
        logger.info("Creazione utente: {} {}", user.getFirstName(), user.getLastName());
        User userEntity = fromRequest(user);
        User savedUser = userRepository.save(userEntity);
        logger.info("Utente creato con successo con id: {}", savedUser.getId());
        return toResponse(savedUser);
    }

    public UserResponse updateUser(Long id, ModifyUserRequest updatedUser) {
        
        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    logger.warn("Utente con id {} non trovato per aggiornamento", id);
                    return new ResourceNotFoundException("User with id " + id + " not found");
                });

        
        logger.info("Aggiornamento utente con id: {}", id);

        if (updatedUser.getFirstName() != null && !updatedUser.getFirstName().isEmpty()) {
            user.setFirstName(updatedUser.getFirstName());
        }
        if (updatedUser.getLastName() != null && !updatedUser.getLastName().isEmpty()) {
            user.setLastName(updatedUser.getLastName());
        }
        if (updatedUser.getEmail() != null && !updatedUser.getEmail().isEmpty()) {
            user.setEmail(updatedUser.getEmail());
        }
        if (updatedUser.getAddress() != null && !updatedUser.getAddress().isEmpty()) {
            user.setAddress(updatedUser.getAddress());
        }
        User updatedEntity = userRepository.save(user);
        logger.info("Utente con id {} aggiornato con successo", id);
        return toResponse(updatedEntity);
    }

    public void deleteUser(Long id) {
        logger.info("Eliminazione utente con id: {}", id);
        if (!userRepository.existsById(id)) {
            logger.warn("Utente con id {} non trovato per eliminazione", id);
            throw new ResourceNotFoundException("User with id " + id + " not found");
        }
        userRepository.deleteById(id);
        logger.info("Utente con id {} eliminato con successo", id);
    }

    @Transactional
    public List<UserResponse> importFromCsv(MultipartFile file) {
        logger.info("Inizio importazione CSV da file: {}", file.getOriginalFilename());
        if (file == null || file.isEmpty()) {
            logger.warn("File CSV non presente o vuoto");
            return List.of();
        }

        List<User> users = new ArrayList<>();
        Pattern emailPattern = Pattern.compile(".+@.+\\..+");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.parse(reader)) {

            List<CSVRecord> records = parser.getRecords();
            if (records.isEmpty()) {
                logger.warn("File CSV vuoto: {}", file.getOriginalFilename());
                return List.of();
            }

            // Detect header automatically: if first record contains header-like tokens or no email-like value, treat as header
            CSVRecord first = records.get(0);
            boolean likelyHeader = false;
            StringBuilder firstConcat = new StringBuilder();
            for (String v : first) {
                firstConcat.append(v == null ? "" : v.toLowerCase()).append(" ");
            }
            String firstLineLower = firstConcat.toString();
            if (firstLineLower.contains("first") || firstLineLower.contains("last") || firstLineLower.contains("name") || firstLineLower.contains("email") || firstLineLower.contains("address") || firstLineLower.contains("nome") || firstLineLower.contains("cognome")) {
                likelyHeader = true;
            } else {
                boolean anyEmailLike = false;
                for (String v : first) {
                    if (v != null && emailPattern.matcher(v).matches()) {
                        anyEmailLike = true;
                        break;
                    }
                }
                likelyHeader = !anyEmailLike;
            }

            int startIndex = likelyHeader ? 1 : 0;
            int lineNumber = startIndex + 1;
            for (int i = startIndex; i < records.size(); i++) {
                CSVRecord rec = records.get(i);
                String firstName = rec.size() > 0 ? rec.get(0).trim() : "";
                String lastName = rec.size() > 1 ? rec.get(1).trim() : "";
                String email = rec.size() > 2 ? rec.get(2).trim() : "";
                String address = rec.size() > 3 ? rec.get(3).trim() : "";

                if (!email.isEmpty() && emailPattern.matcher(email).matches()) {
                    User user = User.builder()
                            .firstName(firstName)
                            .lastName(lastName)
                            .email(email)
                            .address(address)
                            .build();
                    users.add(user);
                    logger.debug("Riga {} elaborata: {} {}", lineNumber, firstName, lastName);
                } else {
                    logger.warn("Riga {} scartata: email vuota o non valida: {}", lineNumber, email);
                }
                lineNumber++;
            }

        } catch (IOException e) {
            logger.error("Errore durante la lettura del file CSV", e);
            throw new CsvImportException("Impossibile leggere il file CSV: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("Formato CSV non valido", e);
            throw new CsvImportException("Formato CSV non valido: " + e.getMessage());
        }

        logger.info("Totale righe elaborate: {}", users.size());
        List<User> savedUsers = userRepository.saveAll(users);
        logger.info("Importazione CSV completata: {} utenti salvati", savedUsers.size());
        return savedUsers.stream().map(this::toResponse).toList();
    }


    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .address(user.getAddress())
                .build();
    }

    private User fromRequest(UserRequest userRequest) {
        return User.builder()
                .firstName(userRequest.getFirstName())
                .lastName(userRequest.getLastName())
                .email(userRequest.getEmail())
                .address(userRequest.getAddress())
                .build();
    }

}
