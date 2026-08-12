package com.vetautet.app.application.user.usecase;

import com.vetautet.app.application.user.dto.AvailabilityCheckType;
import com.vetautet.app.application.user.dto.AvailabilityResult;
import com.vetautet.app.application.user.port.input.CheckUserAvailabilityUseCase;
import com.vetautet.app.application.user.port.output.UserAvailabilityProbe;
import com.vetautet.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks whether a username or email is already taken. Reads the Bloom
 * filter backed {@link UserAvailabilityProbe} when it is synced (fast
 * path); falls back to a direct database existence check otherwise (e.g.
 * right after app startup, before the sync job finishes its first pass).
 * The Bloom filter can false-positive but never false-negatives, so the
 * fallback only exists for correctness while unsynced - it is never needed
 * to correct a wrong "available".
 * Application layer - use case implementation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CheckUserAvailabilityUseCaseImpl implements CheckUserAvailabilityUseCase {

    private final UserAvailabilityProbe userAvailabilityProbe;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public AvailabilityResult execute(AvailabilityCheckType type, String value) {
        String normalized = normalize(value);
        boolean synced = userAvailabilityProbe.isSynced();

        boolean exists = synced
                ? userAvailabilityProbe.mightExist(type, normalized)
                : checkDatabase(type, normalized);

        log.debug("Availability check type={} synced={} exists={}", type, synced, exists);

        return AvailabilityResult.builder()
                .type(type)
                .value(value)
                .available(!exists)
                .build();
    }

    private boolean checkDatabase(AvailabilityCheckType type, String normalized) {
        return switch (type) {
            case USERNAME -> userRepository.existsByUsernameIgnoreCase(normalized);
            case EMAIL -> userRepository.existsByEmailIgnoreCase(normalized);
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
