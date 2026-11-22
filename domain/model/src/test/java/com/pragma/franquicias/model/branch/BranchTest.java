package com.pragma.franquicias.model.branch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pragma.franquicias.model.branch.exceptions.InvalidBranchNameException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class BranchTest {

    private static final String MAX_NAME = "a".repeat(120);
    private static final String TOO_LONG_NAME = "a".repeat(121);

    static String[] invalidNames() {
        return new String[] {null, "", "   ", TOO_LONG_NAME, "  " + TOO_LONG_NAME + "  "};
    }

    static String[] validNames() {
        return new String[] {"a", "  a  ", MAX_NAME, " " + MAX_NAME + " "};
    }

    @Test
    void buildsWithTheNameTrimmed() {
        UUID id = UUID.randomUUID();
        UUID franchiseId = UUID.randomUUID();

        Branch branch = Branch.builder().id(id).franchiseId(franchiseId).name("  Centro  ").build();

        assertEquals(id, branch.getId());
        assertEquals(franchiseId, branch.getFranchiseId());
        assertEquals("Centro", branch.getName());
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsNullBlankOrTooLongNameWhenBuilt(String invalidName) {
        Branch.BranchBuilder builder =
                Branch.builder().id(UUID.randomUUID()).franchiseId(UUID.randomUUID()).name(invalidName);

        assertThrows(InvalidBranchNameException.class, builder::build);
    }

    @ParameterizedTest
    @MethodSource("validNames")
    void acceptsNamesBetweenOneAndOneHundredTwentyCharactersAfterTrim(String name) {
        Branch branch = Branch.builder().id(UUID.randomUUID()).franchiseId(UUID.randomUUID()).name(name).build();

        assertEquals(name.strip(), branch.getName());
    }

    @Test
    void rejectsAMissingId() {
        Branch.BranchBuilder builder = Branch.builder().franchiseId(UUID.randomUUID()).name("Centro");

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void rejectsAMissingFranchiseId() {
        Branch.BranchBuilder builder = Branch.builder().id(UUID.randomUUID()).name("Centro");

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void keepsTheValidationWhenCopiedWithToBuilder() {
        Branch branch = Branch.builder().id(UUID.randomUUID()).franchiseId(UUID.randomUUID()).name("Centro").build();
        Branch.BranchBuilder copy = branch.toBuilder().name("   ");

        assertThrows(InvalidBranchNameException.class, copy::build);
    }
}
