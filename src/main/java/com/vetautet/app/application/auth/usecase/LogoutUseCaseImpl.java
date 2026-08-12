package com.vetautet.app.application.auth.usecase;

import com.vetautet.app.application.auth.port.input.LogoutUseCase;
import com.vetautet.app.domain.auth.model.KeyId;
import com.vetautet.app.domain.auth.repository.RsaKeyPairRepository;
import com.vetautet.app.domain.user.model.UserId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* Use case implementation for user logout
* Application layer - orchestrates domain logic
*/
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class LogoutUseCaseImpl implements LogoutUseCase {

    private final RsaKeyPairRepository rsaKeyPairRepository;

    @Override
    public void execute(String userIdString, String keyIdString) {
        log.info("Executing LogoutUseCase for user ID: {} and key ID: {}", userIdString, keyIdString);

        UserId userId = UserId.of(userIdString);
        KeyId keyId = KeyId.of(keyIdString);

        // Deactivate only the RSA key used by the current authenticated session.
        rsaKeyPairRepository.deactivateByKeyId(keyId, userId);

        log.info("User {} logged out successfully. Token with keyId {} has been revoked.", userIdString, keyIdString);
    }
}
 