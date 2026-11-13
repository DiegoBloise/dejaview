package io.dejaview.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

/**
 * Runs the project's Maven wrapper. Output is streamed to the console while it
 * is produced and a bounded tail is retained for diagnostics. Never throws for
 * a non-zero exit code; callers inspect {@link Result}.
 */
final class ProcessRunner {

    record Result(int exitCode, List<String> outputTail) {
        boolean succeeded() {
            return exitCode == 0;
        }
    }

    /** Runs the project's build. Returns the raw result; never throws. */
    static Result build(Path projectDir, List<String> extraArgs, PrintStream console) {
        List<String> command = new java.util.ArrayList<>(wrapperCommand(projectDir));
        command.add("-B");
        command.addAll(extraArgs);
        if (console != null) {
            console.println("$ " + String.join(" ", command));
        }
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(projectDir.toFile());
        builder.redirectErrorStream(true);
        int exitCode;
        Deque<String> tail = new ArrayDeque<>();
        try {
            Process process = builder.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (console != null) {
                        console.println(line);
                    }
                    tail.addLast(line);
                    if (tail.size() > 400) {
                        tail.pollFirst();
                    }
                }
            }
            exitCode = process.waitFor();
        } catch (IOException e) {
            List<String> failure = List.of("Failed to launch the Maven wrapper: " + e.getMessage());
            if (console != null) {
                failure.forEach(console::println);
            }
            return new Result(127, failure);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new Result(130, List.of("Build interrupted."));
        }
        return new Result(exitCode, new ArrayList<>(tail));
    }

    /** Resolves how to invoke the wrapper on the current platform. */
    static List<String> wrapperCommand(Path projectDir) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("win")) {
            return List.of(projectDir.resolve("mvnw.cmd").toString());
        }
        Path mvnw = projectDir.resolve("mvnw");
        if (Files.isExecutable(mvnw)) {
            return List.of(mvnw.toString());
        }
        return List.of("sh", mvnw.toString());
    }
}
