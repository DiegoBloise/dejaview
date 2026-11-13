package io.dejaview.cli;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Implements {@code dejaview doctor}. Environment and project checks report
 * PASS / WARN / FAIL. FAIL marks the environment unusable for building; WARN
 * marks something suspicious. Exit code is 1 when any FAIL is present.
 */
final class DoctorCommand {

    private DoctorCommand() {
    }

    static int run(Context context, String[] args) {
        boolean portCheck = true;
        for (String arg : args) {
            if (arg.equals("--no-port-check")) {
                portCheck = false;
            } else {
                context.err().println("Unknown option for doctor: " + arg + " (supported: --no-port-check)");
                return 2;
            }
        }
        Counts counts = new Counts();

        check(context, counts, "Java runtime is 25 or newer",
                Runtime.version().feature() >= 25
                        ? Status.pass("JDK " + Runtime.version())
                        : Status.fail("JDK " + Runtime.version() + " detected; DejaView requires JDK 25"));

        check(context, counts, "JDK compiler available",
                java.util.Optional.ofNullable(javax.tools.ToolProvider.getSystemJavaCompiler())
                        .map(c -> Status.pass("javac available"))
                        .orElseGet(() -> Status.fail("no system Java compiler; install a JDK, not just a JRE")));

        Path projectDir = context.cwd();
        boolean unix = !System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path wrapper = projectDir.resolve(unix ? "mvnw" : "mvnw.cmd");
        check(context, counts, "Maven wrapper present",
                Files.exists(wrapper) ? Status.pass(wrapper.getFileName().toString())
                        : Status.fail("missing " + wrapper.getFileName() + "; not a DejaView project?"));
        if (Files.exists(wrapper) && unix && !Files.isExecutable(wrapper)) {
            context.out().println("  WARN  mvnw is not executable; the CLI will fall back to 'sh mvnw'");
            counts.warn++;
        }

        check(context, counts, "pom.xml present",
                Files.exists(projectDir.resolve("pom.xml")) ? Status.pass("pom.xml")
                        : Status.fail("no pom.xml in " + projectDir));

        check(context, counts, "src/main/java present",
                Files.isDirectory(projectDir.resolve("src/main/java")) ? Status.pass("src/main/java")
                        : Status.warn("missing src/main/java; add Java sources there"));

        check(context, counts, "src/main/webapp/index.html present",
                Files.isRegularFile(projectDir.resolve("src/main/webapp/index.html"))
                        ? Status.pass("src/main/webapp/index.html")
                        : Status.fail("missing src/main/webapp/index.html; the dev server and TeaVM need it"));

        try {
            ProjectConfig config = ProjectConfig.load(projectDir);
            context.out().println("  PASS  dejaview.properties parsed (port=" + config.port()
                    + ", host=" + config.host() + ", webappDir=" + config.webappDir() + ")");
            counts.pass++;
            if (portCheck) {
                if (portFree(config.host(), config.port())) {
                    context.out().println("  PASS  port " + config.port() + " on " + config.host() + " is available");
                    counts.pass++;
                } else {
                    context.out().println("  WARN  port " + config.port() + " on " + config.host()
                            + " is already in use; 'dejaview run --port N' to pick another");
                    counts.warn++;
                }
            }
        } catch (CliException e) {
            context.out().println("  FAIL  dejaview.properties: " + e.getMessage());
            counts.fail++;
        }

        Path localRepo = Path.of(System.getProperty("user.home"), ".m2", "repository", "io", "dejaview",
                "dejaview-core", Version.current());
        if (Files.isDirectory(localRepo)) {
            context.out().println("  PASS  DejaView " + Version.current() + " present in the local Maven repository");
            counts.pass++;
        } else {
            context.out().println("  WARN  io.dejaview artifacts for " + Version.current()
                    + " were not found in ~/.m2; built from a repository? run ./mvnw install once first");
            counts.warn++;
        }

        context.out().println();
        context.out().println(counts.pass + " passed, " + counts.warn + " warning(s), " + counts.fail + " failed.");
        return counts.fail > 0 ? 1 : 0;
    }

    private static boolean portFree(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 300);
            return false;
        } catch (IOException e) {
            return true;
        }
    }

    private static void check(Context context, Counts counts, String label, Status status) {
        switch (status.level()) {
            case PASS -> counts.pass++;
            case WARN -> counts.warn++;
            case FAIL -> counts.fail++;
        }
        context.out().println("  " + status.level().name() + "  " + label + (status.detail().isBlank() ? "" : " (" + status.detail() + ")"));
    }

    private record Status(Level level, String detail) {
        static Status pass(String detail) {
            return new Status(Level.PASS, detail);
        }

        static Status warn(String detail) {
            return new Status(Level.WARN, detail);
        }

        static Status fail(String detail) {
            return new Status(Level.FAIL, detail);
        }
    }

    private enum Level {
        PASS, WARN, FAIL
    }

    private static final class Counts {
        int pass;
        int warn;
        int fail;
    }
}
