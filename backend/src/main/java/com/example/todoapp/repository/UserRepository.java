package com.example.todoapp.repository;

import com.example.todoapp.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByExternalIdAndProvider(String externalId, String provider);
    Optional<UserEntity> findByEmail(String email); // Might be useful
}
