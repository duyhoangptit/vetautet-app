package com.vetautet.app.presentation.config;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

import com.vetautet.app.infrastructure.security.CustomJwtAuthenticationConverter;
import com.vetautet.app.infrastructure.security.MultiPortalAuthorizationManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;

import com.nimbusds.jwt.SignedJWT;
import com.vetautet.app.domain.auth.model.KeyId;
import com.vetautet.app.domain.auth.repository.RsaKeyPairRepository;

/**
 * Spring Security configuration for the application.
 * Configures JWT-based authentication with OAuth2 Resource Server.
 * Presentation layer configuration.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final MultiPortalAuthorizationManager multiPortalAuthorizationManager;
    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    /**
     * Configure security filter chain.
     * Defines which endpoints are public and which require authentication.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
                // Disable CSRF for stateless REST API
                .csrf(AbstractHttpConfigurer::disable)

                // Configure session management as stateless
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints - no authentication required
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/api/v1/auth/register/activate",
                                "/api/v1/auth/verify-otp-login",
                                "/api/v1/auth/forgot-password",
                                "/api/v1/auth/reset-password",
                                "/api/v1/users/availability")
                        .permitAll()

                        // Public pre-booking search - users must be able to search
                        // departures before logging in. Scoped to GET only so no other
                        // verb on this path accidentally becomes public. Protected from
                        // abuse by @RateLimit(scope = "departureSearch") on the
                        // controller (see DepartureController) + short-TTL Redis cache
                        // (see SearchDepartureUseCaseImpl), not by authentication.
                        .requestMatchers(HttpMethod.GET, "/api/v1/departures/search")
                        .permitAll()

                        // Swagger/OpenAPI documentation - public access
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**")
                        .permitAll()

                        // Actuator endpoints - public access (consider restricting in production)
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/actuator/prometheus")
                        .permitAll()

                        // any request need to verify RBAC
                        .anyRequest().access(multiPortalAuthorizationManager))

                // Configure OAuth2 Resource Server with JWT
                .oauth2ResourceServer(oauth2 -> oauth2
                        // convert jwt using custom
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(customJwtAuthenticationConverter)));

        return http.build();
    }

    /**
     * JWT Decoder bean for validating JWT tokens.
     * Resolves the RSA public key dynamically from the persisted key id in the
     * JWT header so each login session can use its own signing key.
     */
    @Bean
    public JwtDecoder jwtDecoder(RsaKeyPairRepository rsaKeyPairRepository) {
        return token -> {
            String keyId = extractKeyId(token);
            RSAPublicKey publicKey = rsaKeyPairRepository.findValidKeyById(KeyId.of(keyId), LocalDateTime.now(ZoneId.systemDefault()))
                    .map(rsaKeyPair -> rsaKeyPair.getPublicKey().getPemFormat())
                    .map(this::parsePublicKey)
                    .orElseThrow(() -> new JwtException("No valid RSA public key found for kid: " + keyId));

            return NimbusJwtDecoder.withPublicKey(publicKey).build().decode(token);
        };
    }

    private String extractKeyId(String token) {
        try {
            String keyId = SignedJWT.parse(token).getHeader().getKeyID();
            if (!StringUtils.hasText(keyId)) {
                throw new JwtException("JWT header is missing kid");
            }
            return keyId;
        } catch (ParseException | JwtException ex) {
            throw new JwtException("Failed to parse JWT header", ex);
        }
    }

    private RSAPublicKey parsePublicKey(String pemContent) {
        try {
            String normalizedPem = pemContent
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] decoded = Base64.getDecoder().decode(normalizedPem);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return (RSAPublicKey) keyFactory.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new JwtException("Failed to parse persisted RSA public key", ex);
        }
    }
}
