package com.pragma.franquicias.support;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

// On Linux, SecureRandom reads the operating system entropy source (/dev/urandom) the first time it is used and
// every time it refills its buffer. That read never waits, but BlockHound sees a file read on a non-blocking
// thread. On Windows it does not happen, so only a Linux run (the CI) can fail here.
class BlockHoundSecureRandomTest {

    @Test
    void secureRandomIsAllowedOnANonBlockingThread() {
        StepVerifier.create(Mono.fromSupplier(UUID::randomUUID).subscribeOn(Schedulers.parallel()))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void aRealBlockingCallIsStillDetectedOnANonBlockingThread() {
        Mono<String> sleeping = Mono.fromCallable(() -> {
            Thread.sleep(10);
            return "done";
        });

        StepVerifier.create(sleeping.subscribeOn(Schedulers.parallel()))
                .expectError(BlockingOperationError.class)
                .verify();
    }
}
