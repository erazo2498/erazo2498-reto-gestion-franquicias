package com.pragma.franquicias.api.franchise;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.api.config.JacksonConfig;
import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.model.common.ServiceUnavailableException;
import com.pragma.franquicias.model.franchise.Franchise;
import com.pragma.franquicias.model.franchise.exceptions.DuplicateFranchiseNameException;
import com.pragma.franquicias.model.franchise.exceptions.InvalidFranchiseNameException;
import com.pragma.franquicias.usecase.createfranchise.CreateFranchiseUseCase;
import com.pragma.franquicias.usecase.gettopstockproducts.GetTopStockProductsUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest
@ContextConfiguration(
        classes = {FranchiseRouter.class, FranchiseHandler.class, ApiErrorHandler.class, JacksonConfig.class})
class CreateFranchiseRouteTest {

    private static final String PATH = "/api/v1/franchises";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateFranchiseUseCase createFranchiseUseCase;

    // The handler also needs the query use case to be built.
    @MockitoBean
    private GetTopStockProductsUseCase getTopStockProductsUseCase;

    private WebTestClient.RequestHeadersSpec<?> postJson(String body) {
        return webTestClient.post().uri(PATH).contentType(MediaType.APPLICATION_JSON).bodyValue(body);
    }

    @Test
    void createsTheFranchiseAndAnswersWith201() {
        UUID id = UUID.randomUUID();
        when(createFranchiseUseCase.execute("Burger Co"))
                .thenReturn(Mono.just(Franchise.builder().id(id).name("Burger Co").build()));

        postJson("{\"name\":\"Burger Co\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.name").isEqualTo("Burger Co");
    }

    @Test
    void answersWith400WhenTheNameIsInvalid() {
        when(createFranchiseUseCase.execute("")).thenReturn(Mono.error(new InvalidFranchiseNameException()));

        postJson("{\"name\":\"\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Franchise name must be between 1 and 120 characters")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith400WhenTheBodyIsMissing() {
        when(createFranchiseUseCase.execute(null)).thenReturn(Mono.error(new InvalidFranchiseNameException()));

        webTestClient.post()
                .uri(PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void answersWith400WhenTheJsonIsMalformed() {
        postJson("{\"name\": ")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(createFranchiseUseCase);
    }

    @Test
    void answersWith400WhenTheNameIsNotAText() {
        postJson("{\"name\":123}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(createFranchiseUseCase);
    }

    @Test
    void answersWith409WhenTheNameAlreadyExists() {
        when(createFranchiseUseCase.execute("Burger Co"))
                .thenReturn(Mono.error(new DuplicateFranchiseNameException("Burger Co")));

        postJson("{\"name\":\"Burger Co\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DUPLICATE_NAME")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith415WhenTheContentTypeIsNotJson() {
        webTestClient.post()
                .uri(PATH)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("Burger Co")
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE");

        verifyNoInteractions(createFranchiseUseCase);
    }

    @Test
    void answersWith503WhenThePersistenceIsUnavailable() {
        when(createFranchiseUseCase.execute("Burger Co"))
                .thenReturn(Mono.error(new ServiceUnavailableException()));

        postJson("{\"name\":\"Burger Co\"}")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo("SERVICE_UNAVAILABLE")
                .jsonPath("$.message").isEqualTo("The service is temporarily unavailable, try again later")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith500WithoutLeakingInternalDetailsOnUnexpectedErrors() {
        when(createFranchiseUseCase.execute("Burger Co"))
                .thenReturn(Mono.error(new IllegalStateException("connection refused to 10.0.0.5")));

        postJson("{\"name\":\"Burger Co\"}")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                .jsonPath("$.message").isEqualTo("Unexpected error")
                .jsonPath("$.timestamp").isNotEmpty();
    }
}
