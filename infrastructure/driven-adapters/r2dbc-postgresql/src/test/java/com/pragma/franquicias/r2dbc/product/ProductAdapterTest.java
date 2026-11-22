package com.pragma.franquicias.r2dbc.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.model.branch.exceptions.BranchNotFoundException;
import com.pragma.franquicias.model.product.Product;
import com.pragma.franquicias.model.product.exceptions.DuplicateProductNameException;
import io.r2dbc.spi.R2dbcDataIntegrityViolationException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class ProductAdapterTest {

    private static final String FOREIGN_KEY_VIOLATION = "23503";
    private static final String CHECK_VIOLATION = "23514";

    @Mock
    private R2dbcEntityTemplate template;

    @InjectMocks
    private ProductAdapter adapter;

    private final Product product = Product.builder()
            .id(UUID.randomUUID())
            .branchId(UUID.randomUUID())
            .name("Hamburguesa")
            .stock(10)
            .build();

    private static DataIntegrityViolationException integrityViolation(String sqlState) {
        return new DataIntegrityViolationException(
                "violation", new R2dbcDataIntegrityViolationException("violation", sqlState));
    }

    @Test
    void insertsTheProductAndReturnsTheStoredOne() {
        when(template.insert(any(ProductData.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0, ProductData.class)));

        StepVerifier.create(adapter.save(product))
                .assertNext(saved -> assertEquals(product, saved))
                .verifyComplete();
    }

    @Test
    void translatesAUniqueViolationIntoADuplicateNameError() {
        when(template.insert(any(ProductData.class))).thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

        StepVerifier.create(adapter.save(product))
                .expectError(DuplicateProductNameException.class)
                .verify();
    }

    @Test
    void translatesAForeignKeyViolationIntoABranchNotFoundError() {
        when(template.insert(any(ProductData.class)))
                .thenReturn(Mono.error(integrityViolation(FOREIGN_KEY_VIOLATION)));

        StepVerifier.create(adapter.save(product))
                .expectError(BranchNotFoundException.class)
                .verify();
    }

    @Test
    void doesNotDisguiseOtherIntegrityViolationsAsAMissingBranch() {
        when(template.insert(any(ProductData.class))).thenReturn(Mono.error(integrityViolation(CHECK_VIOLATION)));

        StepVerifier.create(adapter.save(product))
                .expectError(DataIntegrityViolationException.class)
                .verify();
    }

    @Test
    void letsOtherDatabaseErrorsPassUntouched() {
        when(template.insert(any(ProductData.class)))
                .thenReturn(Mono.error(new DataAccessResourceFailureException("connection lost")));

        StepVerifier.create(adapter.save(product))
                .expectError(DataAccessResourceFailureException.class)
                .verify();
    }
}
