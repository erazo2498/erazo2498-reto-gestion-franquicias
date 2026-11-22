package com.pragma.franquicias.model.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pragma.franquicias.model.product.exceptions.InvalidProductNameException;
import com.pragma.franquicias.model.product.exceptions.InvalidProductStockException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class ProductTest {

    private static final String MAX_NAME = "a".repeat(120);
    private static final String TOO_LONG_NAME = "a".repeat(121);

    static String[] invalidNames() {
        return new String[] {null, "", "   ", TOO_LONG_NAME, "  " + TOO_LONG_NAME + "  "};
    }

    static String[] validNames() {
        return new String[] {"a", "  a  ", MAX_NAME, " " + MAX_NAME + " "};
    }

    static Integer[] invalidStocks() {
        return new Integer[] {null, -1, Integer.MIN_VALUE};
    }

    static Integer[] validStocks() {
        return new Integer[] {0, 1, Integer.MAX_VALUE};
    }

    private static Product.ProductBuilder validBuilder() {
        return Product.builder().id(UUID.randomUUID()).branchId(UUID.randomUUID()).name("Hamburguesa").stock(10);
    }

    @Test
    void buildsWithTheNameTrimmed() {
        UUID id = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();

        Product product =
                Product.builder().id(id).branchId(branchId).name("  Hamburguesa  ").stock(10).build();

        assertEquals(id, product.getId());
        assertEquals(branchId, product.getBranchId());
        assertEquals("Hamburguesa", product.getName());
        assertEquals(10, product.getStock());
    }

    @ParameterizedTest
    @MethodSource("invalidNames")
    void rejectsNullBlankOrTooLongNameWhenBuilt(String invalidName) {
        Product.ProductBuilder builder = validBuilder().name(invalidName);

        assertThrows(InvalidProductNameException.class, builder::build);
    }

    @ParameterizedTest
    @MethodSource("validNames")
    void acceptsNamesBetweenOneAndOneHundredTwentyCharactersAfterTrim(String name) {
        Product product = validBuilder().name(name).build();

        assertEquals(name.strip(), product.getName());
    }

    @ParameterizedTest
    @MethodSource("invalidStocks")
    void rejectsAMissingOrNegativeStockWhenBuilt(Integer invalidStock) {
        Product.ProductBuilder builder = validBuilder().stock(invalidStock);

        assertThrows(InvalidProductStockException.class, builder::build);
    }

    @ParameterizedTest
    @MethodSource("validStocks")
    void acceptsAnyStockFromZeroUpwards(Integer stock) {
        Product product = validBuilder().stock(stock).build();

        assertEquals(stock, product.getStock());
    }

    @Test
    void rejectsAMissingId() {
        Product.ProductBuilder builder = Product.builder().branchId(UUID.randomUUID()).name("Hamburguesa").stock(10);

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void rejectsAMissingBranchId() {
        Product.ProductBuilder builder = Product.builder().id(UUID.randomUUID()).name("Hamburguesa").stock(10);

        assertThrows(NullPointerException.class, builder::build);
    }

    @Test
    void keepsTheValidationWhenCopiedWithToBuilder() {
        Product product = validBuilder().build();

        assertThrows(InvalidProductNameException.class, product.toBuilder().name("   ")::build);
        assertThrows(InvalidProductStockException.class, product.toBuilder().stock(-5)::build);
    }
}
