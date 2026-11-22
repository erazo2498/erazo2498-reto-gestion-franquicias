package com.pragma.franquicias.model.franchise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pragma.franquicias.model.franchise.exceptions.InvalidFranchiseNameException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class FranchiseTest {

    private static final String MAX_NAME = "a".repeat(120);
    private static final String TOO_LONG_NAME = "a".repeat(121);

    static String[] invalidNames() {
        return new String[] {null, "", "   ", TOO_LONG_NAME, "  " + TOO_LONG_NAME + "  "};
    }

    @Test
    void buildsWithTheNameTrimmed() {
        UUID id = UUID.randomUUID();

        Franchise franchise = Franchise.builder().id(id).name("  Burger Co  ").build();

        assertEquals(id, franchise.getId());
        assertEquals("Burger Co", franchise.getName());
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsNullBlankOrTooLongNameWhenBuilt(String invalidName) {
        Franchise.FranchiseBuilder builder = Franchise.builder().id(UUID.randomUUID()).name(invalidName);

        assertThrows(InvalidFranchiseNameException.class, builder::build);
    }

    static String[] validNames() {
        return new String[] {"a", "  a  ", MAX_NAME, " " + MAX_NAME + " "};
    }

    @ParameterizedTest
    @MethodSource("validNames")
    void acceptsNamesBetweenOneAndOneHundredTwentyCharactersAfterTrim(String name) {
        Franchise franchise = Franchise.builder().id(UUID.randomUUID()).name(name).build();

        assertEquals(name.strip(), franchise.getName());
    }

    @Test
    void rejectsAMissingId() {
        Franchise.FranchiseBuilder builder = Franchise.builder().name("Burger Co");

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void keepsTheValidationWhenCopiedWithToBuilder() {
        Franchise franchise = Franchise.builder().id(UUID.randomUUID()).name("Burger Co").build();
        Franchise.FranchiseBuilder copy = franchise.toBuilder().name("   ");

        assertThrows(InvalidFranchiseNameException.class, copy::build);
    }
}
