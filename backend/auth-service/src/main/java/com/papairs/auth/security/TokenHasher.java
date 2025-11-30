package com.papairs.auth.security;

public interface TokenHasher {

    /**
     * Hash a plaintext token
     * @param token plaintext token
     * @return hashed token string
     */
    String hash(String token);

    /**
     * Get the expected hash output length
     * Used for database column sizing validation
     * @return hash length in characters
     */
    int getHashLength();
}
