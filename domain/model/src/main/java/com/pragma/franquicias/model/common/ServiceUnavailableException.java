package com.pragma.franquicias.model.common;

// The persistence layer could not serve the request in time (too slow, database down or circuit open).
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException() {
        super("The service is temporarily unavailable, try again later");
    }
}
