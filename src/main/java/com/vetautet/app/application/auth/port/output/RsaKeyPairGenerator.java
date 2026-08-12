package com.vetautet.app.application.auth.port.output;

import java.security.KeyPair;

/**
 * Output port for RSA key pair generation
 * This will be implemented by infrastructure layer
 */
public interface RsaKeyPairGenerator {

    /**
     * Generate RSA key pair
     *
     * @param keySize Key size in bits (2048, 4096, etc.)
     * @return Generated KeyPair
     */
    KeyPair generateKeyPair(int keySize);

    /**
     * Convert public key to PEM format
     *
     * @param keyPair KeyPair containing public key
     * @return Public key in PEM format
     */
    String convertToPemFormat(KeyPair keyPair);
}
 