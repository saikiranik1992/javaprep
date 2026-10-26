package com.architect.interviewportal.service;

import com.architect.interviewportal.model.IdeExecutionRequest;
import com.architect.interviewportal.model.IdeExecutionResponse;
import org.springframework.stereotype.Service;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class IdeExecutionService {

    private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("\\bpublic\\s+class\\s+([A-Za-z_][A-Za-z0-9_]*)");
    private static final Pattern ANY_CLASS_PATTERN = Pattern.compile("\\bclass\\s+([A-Za-z_][A-Za-z0-9_]*)");

    private final Object executionLock = new Object();

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
        String runtimeLabel = "OpenJDK " + hostJavaVersion + " (In-Memory Fast Compiler · Java " + requestedJava + ") · Spring Boot " + requestedBoot;

        try {
            String sourceCode = request.sourceCode() != null ? request.sourceCode() : "";
            String entryClassName = detectEntryClassName(sourceCode);

            tempDir = Files.createTempDirectory("architect-ide-");
            Path sourceFile = tempDir.resolve(entryClassName + ".java");
            Files.writeString(sourceFile, sourceCode, StandardCharsets.UTF_8);

            // Fast Path: In-process compilation via javax.tools.JavaCompiler (0 subprocess overhead, ~30ms on Render Free Tier)
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler != null) {
                return compileAndRunInProcess(compiler, tempDir, sourceFile, entryClassName, runtimeLabel, start);
            }

            // Fallback Path: Subprocess execution with lightweight flags (-XX:TieredStopAtLevel=1 -Xms16m -Xmx64m) and 30s timeout
            return executeInLightweightSubprocess(tempDir, sourceFile, runtimeLabel, start);
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

    private IdeExecutionResponse compileAndRunInProcess(
            JavaCompiler compiler,
            Path tempDir,
            Path sourceFile,
            String entryClassName,
            String runtimeLabel,
            long start
    ) throws Exception {
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(List.of(sourceFile.toFile()));
            List<String> options = List.of("-encoding", "UTF-8", "-d", tempDir.toAbsolutePath().toString());

            Boolean compiledOk = compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();
            if (compiledOk == null || !compiledOk) {
                StringBuilder err = new StringBuilder();
                for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                    err.append("Line ")
                       .append(d.getLineNumber())
                       .append(": ")
                       .append(d.getMessage(Locale.ENGLISH))
                       .append("\n");
                }
                return new IdeExecutionResponse(
                        false,
                        runtimeLabel,
                        System.currentTimeMillis() - start,
                        "",
                        err.toString().isBlank() ? "Compilation failed." : err.toString()
                );
            }
        }

        // Execute the compiled class inside an isolated URLClassLoader with a 20-second timeout
        ByteArrayOutputStream stdoutBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBuffer = new ByteArrayOutputStream();

        synchronized (executionLock) {
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            InputStream originalIn = System.in;

            ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ide-sandbox-runner");
                t.setDaemon(true);
                return t;
            });

            try (PrintStream capturedOut = new PrintStream(stdoutBuffer, true, StandardCharsets.UTF_8);
                 PrintStream capturedErr = new PrintStream(stderrBuffer, true, StandardCharsets.UTF_8);
                 URLClassLoader classLoader = new URLClassLoader(new URL[]{tempDir.toUri().toURL()}, ClassLoader.getPlatformClassLoader())) {

                System.setOut(capturedOut);
                System.setErr(capturedErr);
                System.setIn(new ByteArrayInputStream(new byte[0]));

                Future<?> future = executor.submit(() -> {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    try {
                        Class<?> mainClass = Class.forName(entryClassName, true, classLoader);
                        Method mainMethod = mainClass.getMethod("main", String[].class);
                        mainMethod.setAccessible(true);
                        mainMethod.invoke(null, (Object) new String[0]);
                    } catch (InvocationTargetException ite) {
                        Throwable cause = ite.getTargetException() != null ? ite.getTargetException() : ite;
                        cause.printStackTrace(capturedErr);
                    } catch (Throwable t) {
                        t.printStackTrace(capturedErr);
                    }
                });

                try {
                    future.get(20, TimeUnit.SECONDS);
                } catch (TimeoutException te) {
                    future.cancel(true);
                    return new IdeExecutionResponse(
                            false,
                            runtimeLabel,
                            System.currentTimeMillis() - start,
                            stdoutBuffer.toString(StandardCharsets.UTF_8),
                            "Execution timed out after 20 seconds (check for infinite loops or blocking waits)."
                    );
                } catch (ExecutionException ee) {
                    Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
                    cause.printStackTrace(capturedErr);
                }
            } finally {
                System.setOut(originalOut);
                System.setErr(originalErr);
                System.setIn(originalIn);
                executor.shutdownNow();
            }
        }

        String stdout = stdoutBuffer.toString(StandardCharsets.UTF_8);
        String stderr = stderrBuffer.toString(StandardCharsets.UTF_8);
        long elapsed = System.currentTimeMillis() - start;

        return new IdeExecutionResponse(
                stderr.isBlank(),
                runtimeLabel,
                elapsed,
                stdout,
                stderr
        );
    }

    private IdeExecutionResponse executeInLightweightSubprocess(
            Path tempDir,
            Path sourceFile,
            String runtimeLabel,
            long start
    ) throws Exception {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        if (!Files.isExecutable(Path.of(javaBin))) {
            javaBin = "java";
        }

        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-XX:TieredStopAtLevel=1",
                "-XX:+UseSerialGC",
                "-Xms16m",
                "-Xmx64m",
                "-Dfile.encoding=UTF-8",
                sourceFile.toAbsolutePath().toString()
        );
        pb.directory(tempDir.toFile());
        sanitizeEnvironment(pb.environment());
        Process process = pb.start();
        process.getOutputStream().close();

        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new IdeExecutionResponse(
                    false,
                    runtimeLabel,
                    System.currentTimeMillis() - start,
                    "",
                    "Execution timed out after 30 seconds."
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
    }

    private String detectEntryClassName(String sourceCode) {
        Matcher pubMatcher = PUBLIC_CLASS_PATTERN.matcher(sourceCode);
        if (pubMatcher.find()) {
            return pubMatcher.group(1);
        }
        Matcher anyMatcher = ANY_CLASS_PATTERN.matcher(sourceCode);
        if (anyMatcher.find()) {
            return anyMatcher.group(1);
        }
        return "Main";
    }

    private void sanitizeEnvironment(Map<String, String> env) {
        if (env == null) return;
        env.remove("JDK_JAVA_OPTIONS");
        env.remove("JAVA_TOOL_OPTIONS");
        env.remove("_JAVA_OPTIONS");
    }
}
