package com.pragma.franquicias.model.franchise;

import com.pragma.franquicias.model.franchise.exceptions.InvalidFranchiseNameException;
import java.util.Optional;

public final class FranchiseValidator {

    public static final int MAX_NAME_LENGTH = 120;

    private FranchiseValidator() {
    }

    public static String validName(String name) {
        return Optional.ofNullable(name)
                .map(String::strip)
                .filter(stripped -> !stripped.isEmpty() && stripped.length() <= MAX_NAME_LENGTH)
                .orElseThrow(InvalidFranchiseNameException::new);
    }
}
