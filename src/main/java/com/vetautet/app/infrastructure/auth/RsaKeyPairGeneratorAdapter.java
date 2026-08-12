package com.vetautet.app.infrastructure.auth;

import com.vetautet.app.application.auth.port.output.RsaKeyPairGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
* Infrastructure implementation of RSA Key Pair Generator
* Handles cryptographic operations
*/
@Component
@Slf4j
public class RsaKeyPairGeneratorAdapter implements RsaKeyPairGenerator {

    @Override
    public KeyPair generateKeyPair(int keySize) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            log.debug("Generated RSA key pair with key size: {}", keySize);
            return keyPair;

        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate RSA key pair", e);
            throw new RuntimeException("Failed to generate RSA key pair", e);
        }
    }

    @Override
    public String convertToPemFormat(KeyPair keyPair) {
        try {
            RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
            byte[] publicKeyBytes = publicKey.getEncoded();
            String base64PublicKey = Base64.getEncoder().encodeToString(publicKeyBytes);

            // Format as PEM
            StringBuilder pem = new StringBuilder();
            pem.append("-----BEGIN PUBLIC KEY-----\n");

            // Split into 64-character lines
            int lineLength = 64;
            for (int i = 0; i < base64PublicKey.length(); i += lineLength) {
                int endIndex = Math.min(i + lineLength, base64PublicKey.length());
                pem.append(base64PublicKey, i, endIndex).append("\n");
            }

            pem.append("-----END PUBLIC KEY-----");

            return pem.toString();

        } catch (Exception e) {
            log.error("Failed to convert public key to PEM format", e);
            throw new RuntimeException("Failed to convert public key to PEM format", e);
        }
    }
}
 