package com.vetautet.app.infrastructure.persistence.jpa.adapter;

import com.vetautet.app.infrastructure.persistence.jpa.repository.UserJpaRepository;
import com.vetautet.app.infrastructure.persistence.mapper.UserEntityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterExistsByIgnoreCaseTest {

    @Mock
    private UserJpaRepository jpaRepository;

    @Mock
    private UserEntityMapper mapper;

    private UserRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new UserRepositoryAdapter(jpaRepository, mapper);
    }

    @Test
    void existsByUsernameIgnoreCase_delegatesToJpaRepository() {
        when(jpaRepository.existsByUsernameIgnoreCase("john_doe")).thenReturn(true);

        assertThat(adapter.existsByUsernameIgnoreCase("john_doe")).isTrue();
    }

    @Test
    void existsByEmailIgnoreCase_delegatesToJpaRepository() {
        when(jpaRepository.existsByEmailIgnoreCase("john@example.com")).thenReturn(false);

        assertThat(adapter.existsByEmailIgnoreCase("john@example.com")).isFalse();
    }
}
