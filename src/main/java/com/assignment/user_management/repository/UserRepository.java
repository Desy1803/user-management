package com.assignment.user_management.repository;

import com.assignment.user_management.entity.User;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query(value = "SELECT * FROM users u WHERE " +
       "(:firstName IS NULL OR LOWER(u.first_name) = LOWER(:firstName)) AND " +
       "(:lastName IS NULL OR LOWER(u.last_name) = LOWER(:lastName))", 
       // Per le query native impaginate che usano @Query, serve dire a Spring come calcolare il totale dei record
       countQuery = "SELECT count(*) FROM users u WHERE (:firstName IS NULL OR LOWER(u.first_name) = LOWER(:firstName)) AND (:lastName IS NULL OR LOWER(u.last_name) = LOWER(:lastName))",
       nativeQuery = true) 
    Page<User> findByFirstNameAndLastName(
        @Param("firstName") String firstName, 
        @Param("lastName") String lastName,
        Pageable pageable // <-- FONDAMENTALE: Passalo qui!
    );
}