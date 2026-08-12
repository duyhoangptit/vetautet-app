package com.vetautet.app.application.auth.service;

import java.security.KeyPair;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

import com.vetautet.app.application.auth.dto.TokenDto;
import com.vetautet.app.application.auth.port.output.JwtTokenGenerator;
import com.vetautet.app.application.auth.port.output.RsaKeyPairGenerator;
import com.vetautet.app.domain.auth.model.KeyId;
import com.vetautet.app.domain.auth.model.PublicKey;
import com.vetautet.app.domain.auth.model.RsaKeyPair;
import com.vetautet.app.domain.auth.repository.RsaKeyPairRepository;
import com.vetautet.app.domain.auth.service.AuthDomainService;
import com.vetautet.app.domain.user.model.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthTokenIssueService {

    private static final int DEFAULT_TOKEN_EXPIRATION_MINUTES = 60;
    private static final int DEFAULT_KEY_SIZE = 2048;

    private final RsaKeyPairRepository rsaKeyPairRepository;
    private final AuthDomainService authDomainService;
    private final RsaKeyPairGenerator keyPairGenerator;
    private final JwtTokenGenerator tokenGenerator;

    public TokenDto issueToken(User user, String portal) {
        authDomainService.validateTokenExpiration(DEFAULT_TOKEN_EXPIRATION_MINUTES);

        Instant expiresAt = Instant.now().plus(DEFAULT_TOKEN_EXPIRATION_MINUTES, ChronoUnit.MINUTES);
        LocalDateTime keyExpiresAt = LocalDateTime.now(ZoneId.systemDefault()).plusMinutes(DEFAULT_TOKEN_EXPIRATION_MINUTES);

        KeyPair keyPair = keyPairGenerator.generateKeyPair(DEFAULT_KEY_SIZE);
        String publicKeyPem = keyPairGenerator.convertToPemFormat(keyPair);

        KeyId keyId = KeyId.generate();
        RsaKeyPair rsaKeyPair = RsaKeyPair.builder()
                .keyId(keyId)
                .userId(user.getUserId())
                .publicKey(PublicKey.of(publicKeyPem))
                .algorithm("RSA")
                .keySize(DEFAULT_KEY_SIZE)
                .isActive(true)
                .expiresAt(keyExpiresAt)
                .build();

        authDomainService.validateKeyPair(rsaKeyPair);
        rsaKeyPairRepository.save(rsaKeyPair);

        String token = tokenGenerator.generateToken(
                user,
                portal,
                keyPair,
                keyId.getValue(),
                DEFAULT_TOKEN_EXPIRATION_MINUTES);

        return TokenDto.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .expiresAt(expiresAt)
                .keyId(keyId.getValue())
                .build();
    }
}