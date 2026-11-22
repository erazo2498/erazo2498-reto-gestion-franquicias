package com.pragma.franquicias.support;

import reactor.blockhound.BlockHound;
import reactor.blockhound.integration.BlockHoundIntegration;

// On Linux, SecureRandom reads the operating system entropy source (/dev/urandom). That read never waits for data,
// but BlockHound sees a file read on a non-blocking thread. It happens in UUID.randomUUID() and inside the
// PostgreSQL driver, which generates a random nonce every time the pool opens a connection (SCRAM authentication).
// The driver code cannot be moved to another thread, so the allowance is narrow: only inside SecureRandom.nextBytes.
// BlockHound keeps watching every other blocking call (see BlockHoundSecureRandomTest).
public class SecureRandomBlockHoundIntegration implements BlockHoundIntegration {

    @Override
    public void applyTo(BlockHound.Builder builder) {
        builder.allowBlockingCallsInside("java.security.SecureRandom", "nextBytes");
    }
}
