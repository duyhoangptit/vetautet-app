package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.domain.user.model.UserId;
import com.vetautet.app.domain.user.repository.UserRepository;
import com.vetautet.app.infrastructure.persistence.jpa.entity.UserJpaEntity;
import com.vetautet.app.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.UserEntityMapper;
import com.vetautet.app.shared.common.util.PageableSanitizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adapter implementing the domain UserRepository interface
 * Bridges domain layer with infrastructure (JPA)
 * This is the hexagonal architecture adapter pattern
 */
@Component
@RequiredArgsConstructor
@Transactional
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserEntityMapper mapper;

    @Override
    public User save(User user) {
        UserJpaEntity entity;

        if (user.getUserId() != null) {
            // Update existing user
            entity = jpaRepository.findById(user.getUserId().getValue())
                    .orElseGet(() -> mapper.toEntity(user));
            mapper.updateEntity(entity, user);
        } else {
            // Create new user
            entity = mapper.toEntity(user);
        }

        UserJpaEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(UserId userId) {
        return jpaRepository.findById(userId.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.getValue())
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> findAll(Pageable pageable) {
        return jpaRepository.findAll(PageableSanitizer.capped(pageable))
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<User> searchByKeyword(String keyword, Pageable pageable) {
        return jpaRepository.searchUsers(keyword, PageableSanitizer.capped(pageable))
                .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsernameIgnoreCase(String username) {
        return jpaRepository.existsByUsernameIgnoreCase(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmailIgnoreCase(String email) {
        return jpaRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public void delete(UserId userId) {
        jpaRepository.deleteById(userId.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(UserId userId) {
        return jpaRepository.existsById(userId.getValue());
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return jpaRepository.count();
    }
}
 