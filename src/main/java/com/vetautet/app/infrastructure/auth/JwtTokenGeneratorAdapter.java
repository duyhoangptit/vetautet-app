package com.vetautet.app.infrastructure.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import com.vetautet.app.application.auth.port.output.JwtTokenGenerator;
import com.vetautet.app.domain.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Infrastructure implementation of JWT Token Generator
 * Uses Spring Security OAuth2 JWT library
 */
@Component
@Slf4j
public class JwtTokenGeneratorAdapter implements JwtTokenGenerator {

    private static final String ISSUER = "fsoft-api";
    private static final List<String> AUDIENCE = List.of("api-client");

    @Override
    public String generateToken(User user, String portal, KeyPair keyPair, String keyId, int expirationMinutes) {
        try {
            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

            // Create JWT encoder with RSA key pair
            RSAKey rsaKey = new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(keyId)
                    .build();

            JWKSet jwkSet = new JWKSet(rsaKey);
            JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(jwkSet);
            JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);

            // Build JWT claims
            Instant now = Instant.now();
            Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

            JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                    .id(UUID.randomUUID().toString())
                    .issuer(ISSUER)
                    .subject(user.getUserId().getValue().toString())
                    .audience(AUDIENCE)
                    .issuedAt(now)
                    .expiresAt(expiration)
                    .claim("kid", keyId)
                    .claim("email", user.getEmail().getValue())
                    .claim("name", user.getPersonalInfo().getFullName())
                    .claim("status", user.getStatus().getCode())
                    .claim("portal", portal);

            if (user.getRoles() != null) {
                claimsBuilder.claim("roles", user.getRoles());
            }

            // Add optional claims
            if (user.getUsername() != null) {
                claimsBuilder.claim("username", user.getUsername());
            }

            JwtClaimsSet claims = claimsBuilder.build();

            // Sign and encode JWT
            String token = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();

            log.info("Generated JWT token for user: {} with keyId: {}",
                    user.getUserId().getValue(), keyId);

            return token;

        } catch (Exception e) {
            log.error("Failed to generate JWT token for user: {}",
                    user.getUserId().getValue(), e);
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }
}
 