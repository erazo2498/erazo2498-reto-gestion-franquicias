package com.pragma.franquicias.api.branch;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pragma.franquicias.api.config.JacksonConfig;
import com.pragma.franquicias.api.error.ApiErrorHandler;
import com.pragma.franquicias.model.branch.Branch;
import com.pragma.franquicias.model.branch.exceptions.DuplicateBranchNameException;
import com.pragma.franquicias.model.branch.exceptions.InvalidBranchNameException;
import com.pragma.franquicias.model.franchise.exceptions.FranchiseNotFoundException;
import com.pragma.franquicias.usecase.createbranch.CreateBranchUseCase;
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
@ContextConfiguration(classes = {BranchRouter.class, BranchHandler.class, ApiErrorHandler.class, JacksonConfig.class})
class CreateBranchRouteTest {

    private static final UUID FRANCHISE_ID = UUID.randomUUID();
    private static final String PATH = "/api/v1/franchises/{franchiseId}/branches";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateBranchUseCase createBranchUseCase;

    private WebTestClient.RequestHeadersSpec<?> postJson(Object franchiseId, String body) {
        return webTestClient.post().uri(PATH, franchiseId).contentType(MediaType.APPLICATION_JSON).bodyValue(body);
    }

    @Test
    void createsTheBranchAndAnswersWith201() {
        UUID id = UUID.randomUUID();
        when(createBranchUseCase.execute(FRANCHISE_ID, "Centro"))
                .thenReturn(Mono.just(Branch.builder().id(id).franchiseId(FRANCHISE_ID).name("Centro").build()));

        postJson(FRANCHISE_ID, "{\"name\":\"Centro\"}")
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.id").isEqualTo(id.toString())
                .jsonPath("$.franchiseId").isEqualTo(FRANCHISE_ID.toString())
                .jsonPath("$.name").isEqualTo("Centro");
    }

    @Test
    void answersWith400WhenTheNameIsInvalid() {
        when(createBranchUseCase.execute(FRANCHISE_ID, "")).thenReturn(Mono.error(new InvalidBranchNameException()));

        postJson(FRANCHISE_ID, "{\"name\":\"\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Branch name must be between 1 and 120 characters")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith400WhenTheBodyIsMissing() {
        when(createBranchUseCase.execute(FRANCHISE_ID, null)).thenReturn(Mono.error(new InvalidBranchNameException()));

        webTestClient.post()
                .uri(PATH, FRANCHISE_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");
    }

    @Test
    void answersWith400WhenTheJsonIsMalformed() {
        postJson(FRANCHISE_ID, "{\"name\": ")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(createBranchUseCase);
    }

    @Test
    void answersWith400WhenTheNameIsNotAText() {
        postJson(FRANCHISE_ID, "{\"name\":123}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA");

        verifyNoInteractions(createBranchUseCase);
    }

    @Test
    void answersWith400WhenTheFranchiseIdIsNotAUuid() {
        postJson("abc", "{\"name\":\"Centro\"}")
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_DATA")
                .jsonPath("$.message").isEqualTo("Franchise id must be a valid UUID")
                .jsonPath("$.timestamp").isNotEmpty();

        verifyNoInteractions(createBranchUseCase);
    }

    @Test
    void answersWith404WhenTheFranchiseDoesNotExist() {
        when(createBranchUseCase.execute(FRANCHISE_ID, "Centro"))
                .thenReturn(Mono.error(new FranchiseNotFoundException(FRANCHISE_ID)));

        postJson(FRANCHISE_ID, "{\"name\":\"Centro\"}")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.code").isEqualTo("FRANCHISE_NOT_FOUND")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith409WhenTheNameAlreadyExistsInTheFranchise() {
        when(createBranchUseCase.execute(FRANCHISE_ID, "Centro"))
                .thenReturn(Mono.error(new DuplicateBranchNameException("Centro")));

        postJson(FRANCHISE_ID, "{\"name\":\"Centro\"}")
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("DUPLICATE_NAME")
                .jsonPath("$.timestamp").isNotEmpty();
    }

    @Test
    void answersWith415WhenTheContentTypeIsNotJson() {
        webTestClient.post()
                .uri(PATH, FRANCHISE_ID)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("Centro")
                .exchange()
                .expectStatus().isEqualTo(415)
                .expectBody()
                .jsonPath("$.code").isEqualTo("UNSUPPORTED_MEDIA_TYPE");

        verifyNoInteractions(createBranchUseCase);
    }

    @Test
    void answersWith500WithoutLeakingInternalDetailsOnUnexpectedErrors() {
        when(createBranchUseCase.execute(FRANCHISE_ID, "Centro"))
                .thenReturn(Mono.error(new IllegalStateException("connection refused to 10.0.0.5")));

        postJson(FRANCHISE_ID, "{\"name\":\"Centro\"}")
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INTERNAL_ERROR")
                .jsonPath("$.message").isEqualTo("Unexpected error")
                .jsonPath("$.timestamp").isNotEmpty();
    }
}
