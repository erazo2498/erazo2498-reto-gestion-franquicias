package com.pragma.franquicias.rules;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coding rules that are checked by reading the source code. This file quotes the forbidden
 * patterns, so the scan skips the {@code rules} package. Each failure names the file, the line and how to fix it.
 */
class SourceRulesTest {

    private static final Path ROOT = Path.of(".");

    private static final Pattern BLOCK = Pattern.compile("\\.(block|blockFirst|blockLast)\\s*\\(");
    private static final Pattern STREAMS = Pattern.compile("java\\.util\\.stream|\\.stream\\s*\\(\\)|\\bCollectors\\b");
    private static final Pattern TRY_CATCH = Pattern.compile("\\btry\\s*[({]|\\bcatch\\s*\\(");
    private static final Pattern IF = Pattern.compile("\\bif\\s*\\(");
    private static final Pattern JAVAX = Pattern.compile("^\\s*import\\s+javax\\.", Pattern.MULTILINE);
    private static final Pattern AWS_KEY = Pattern.compile("AKIA[0-9A-Z]{16}");
    private static final Pattern PLAIN_PASSWORD = Pattern.compile(
            "(?i)(password|secret|apikey|api-key|token)\\s*[:=]\\s*[\"']?(?!\\$\\{|\\s|\"|')[^\\s\"'#]{3,}");

    @Test
    void noBlockingCalls() throws IOException {
        assertNoMatches(sources("main"), BLOCK, "no-blocking", "do not call .block(); return the Mono/Flux and compose it");
        assertNoMatches(sources("test"), BLOCK, "no-blocking", "use StepVerifier instead of .block() in tests");
    }

    @Test
    void noJavaStreamsInProductionCode() throws IOException {
        assertNoMatches(sources("main"), STREAMS, "no-java-streams", "use Flux operators instead of java.util.stream");
    }

    @Test
    void noTryCatchInProductionCode() throws IOException {
        assertNoMatches(sources("main"), TRY_CATCH, "no-try-catch",
                "handle errors with onErrorResume / onErrorMap / onErrorReturn");
    }

    @Test
    void noIfInUseCasesAndEntryPoints() throws IOException {
        var files = sources("main").stream()
                .filter(f -> isIn(f, "domain/usecase") || isIn(f, "infrastructure/entry-points"))
                .toList();
        assertNoMatches(files, IF, "no-if-flow",
                "use filter / switchIfEmpty / flatMap; data validation belongs in the domain entity");
    }

    @Test
    void noJavaxImports() throws IOException {
        assertNoMatches(sources("main"), JAVAX, "no-javax", "use jakarta.* instead of javax.*");
    }

    @Test
    void noPlainTextCredentials() throws IOException {
        var files = new ArrayList<>(sources("main"));
        files.addAll(resources());
        assertNoMatches(files, AWS_KEY, "no-secrets", "never commit an AWS access key");
        assertNoMatches(files, PLAIN_PASSWORD, "no-secrets", "use ${PLACEHOLDER} filled from the environment or Secrets Manager");
    }

    private static boolean isIn(Path file, String module) {
        return file.toString().replace('\\', '/').contains(module + "/");
    }

    private static List<Path> sources(String set) throws IOException {
        return find("src/" + set + "/java", ".java");
    }

    private static List<Path> resources() throws IOException {
        return find("src/main/resources", ".yaml", ".yml", ".properties");
    }

    private static List<Path> find(String folder, String... extensions) throws IOException {
        try (Stream<Path> walk = Files.walk(ROOT)) {
            return walk
                    .filter(Files::isRegularFile)
                    .map(p -> p.toString().replace('\\', '/'))
                    .filter(p -> p.contains("/" + folder + "/"))
                    .filter(p -> !p.contains("/build/") && !p.contains("/rules/"))
                    .filter(p -> Stream.of(extensions).anyMatch(p::endsWith))
                    .map(Path::of)
                    .toList();
        }
    }

    private static void assertNoMatches(List<Path> files, Pattern pattern, String rule, String howToFix)
            throws IOException {
        var problems = new ArrayList<String>();
        for (var file : files) {
            var lines = Files.readAllLines(file);
            for (var i = 0; i < lines.size(); i++) {
                if (pattern.matcher(lines.get(i)).find()) {
                    problems.add(file + ":" + (i + 1) + " -> " + lines.get(i).strip());
                }
            }
        }
        assertThat(problems)
                .as("%s violated. How to fix: %s%n%s", rule, howToFix, String.join("\n", problems))
                .isEmpty();
    }
}
