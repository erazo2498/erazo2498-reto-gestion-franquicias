package com.pragma.franquicias.model.branch;

import com.pragma.franquicias.model.branch.exceptions.InvalidBranchNameException;
import java.util.Optional;

public final class BranchValidator {

    public static final int MAX_NAME_LENGTH = 120;

    private BranchValidator() {
    }

    public static String validName(String name) {
        return Optional.ofNullable(name)
                .map(String::strip)
                .filter(stripped -> !stripped.isEmpty() && stripped.length() <= MAX_NAME_LENGTH)
                .orElseThrow(InvalidBranchNameException::new);
    }
}
