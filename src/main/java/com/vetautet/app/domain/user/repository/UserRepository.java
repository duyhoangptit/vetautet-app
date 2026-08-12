package com.vetautet.app.domain.user.repository;

import com.vetautet.app.domain.user.model.Email;
import com.vetautet.app.domain.user.model.User;
import com.vetautet.app.domain.user.model.UserId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * Repository interface for User domain
 * Domain layer - port (will be implemented by infrastructure layer)
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId userId);

    Optional<User> findByEmail(Email email);

    Page<User> findAll(Pageable pageable);

    Page<User> searchByKeyword(String keyword, Pageable pageable);

    boolean existsByEmail(Email email);

    void delete(UserId userId);

    boolean existsById(UserId userId);

    long count();
}
 