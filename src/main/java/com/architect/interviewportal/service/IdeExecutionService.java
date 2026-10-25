package com.architect.interviewportal.service;

import com.architect.interviewportal.model.IdeExecutionRequest;
import com.architect.interviewportal.model.IdeExecutionResponse;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class IdeExecutionService {

    public IdeExecutionResponse executeCode(IdeExecutionRequest request) {
        long start = System.currentTimeMillis();
        Path tempDir = null;
        String hostJavaVersion = System.getProperty("java.version", "21");
        String requestedJava = (request.javaVersion() != null && !request.javaVersion().isBlank())
                ? request.javaVersion()
                : hostJavaVersion;
        String requestedBoot = (request.springBootVersion() != null && !request.springBootVersion().isBlank())
                ? request.springBootVersion()
                : "3.5.0 (Latest Stable)";
        String runtimeLabel = "OpenJDK " + hostJavaVersion + " (Java " + requestedJava + " mode) · Spring Boot " + requestedBoot;

        try {
            tempDir = Files.createTempDirectory("architect-ide-");
            Path sourceFile = tempDir.resolve("Main.java");
            Files.writeString(sourceFile, request.sourceCode(), StandardCharsets.UTF_8);

            // Resolve the exact java launcher binary from current JVM java.home
            String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
            if (!Files.isExecutable(Path.of(javaBin))) {
                javaBin = "java";
            }

            // Execute Main.java directly via JEP 330 single-file source launcher WITHOUT hardcoded --source 25
            // so it works seamlessly on OpenJDK 11, 17, 21, 23.0.2, 24, and 25.
            ProcessBuilder pb = new ProcessBuilder(
                    javaBin, "-Dfile.encoding=UTF-8", sourceFile.toAbsolutePath().toString()
            );
            pb.directory(tempDir.toFile());
            sanitizeEnvironment(pb.environment());
            Process process = pb.start();

            boolean finished = process.waitFor(8, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new IdeExecutionResponse(
                        false,
                        runtimeLabel,
                        System.currentTimeMillis() - start,
                        "",
                        "Execution timed out after 8 seconds."
                );
            }

            String stdout = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String stderr = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            long elapsed = System.currentTimeMillis() - start;

            return new IdeExecutionResponse(
                    process.exitValue() == 0,
                    runtimeLabel,
                    elapsed,
                    stdout,
                    stderr
            );
        } catch (Exception e) {
            return new IdeExecutionResponse(false, runtimeLabel, System.currentTimeMillis() - start, "", e.getMessage());
        } finally {
            if (tempDir != null) {
                try (var stream = Files.walk(tempDir)) {
                    stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
                } catch (Exception ignored) {}
            }
        }
    }

    private void sanitizeEnvironment(Map<String, String> env) {
        if (env == null) return;
        env.remove("JDK_JAVA_OPTIONS");
        env.remove("JAVA_TOOL_OPTIONS");
        env.remove("_JAVA_OPTIONS");
    }
}
