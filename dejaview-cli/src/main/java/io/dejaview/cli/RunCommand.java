package io.dejaview.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/** Implements {@code dejaview run}: build once, then serve with live reload while watching sources. */
final class RunCommand {

    private RunCommand() {
    }

    static int run(Context context, String[] args) throws CliException {
        Path projectDir = context.cwd();
        BuildCommand.ensureProject(projectDir);
        ProjectConfig config = ProjectConfig.load(projectDir);
        String host = config.host();
        int port = config.port();
        boolean skipBuild = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--port" -> port = parsePort(requireValue(args, ++i, "--port"));
                case "--host" -> host = requireValue(args, ++i, "--host");
                case "--no-build" -> skipBuild = true;
                default -> {
                    if (arg.startsWith("--port=")) {
                        port = parsePort(arg.substring("--port=".length()));
                    } else if (arg.startsWith("--host=")) {
                        host = arg.substring("--host=".length());
                    } else {
                        throw new CliException("Unknown option for run: " + arg
                                + " (supported: --port, --host, --no-build)");
                    }
                }
            }
        }
        Path webRoot = config.webappDir().isAbsolute()
                ? config.webappDir().normalize()
                : projectDir.resolve(config.webappDir()).normalize();

        if (!skipBuild) {
            context.out().println("Building the project (this runs ./mvnw package)...");
            ProcessRunner.Result result = ProcessRunner.build(projectDir, List.of("package"), context.out());
            if (!result.succeeded()) {
                BuildCommand.printDiagnostics(context, result.outputTail());
                return 1;
            }
        }
        if (!Files.isDirectory(webRoot)) {
            throw new CliException("The web root " + webRoot + " does not exist. Run 'dejaview build' first.");
        }

        final DevServer server;
        try {
            server = DevServer.create(host, port, webRoot);
        } catch (IOException e) {
            throw new CliException("Could not start the development server on " + host + ":" + port + ": " + e.getMessage()
                    + ". Use --port to pick another port.");
        }
        server.start();

        BuildWatcher watcher;
        try {
            watcher = new BuildWatcher(projectDir, () -> {
                context.out().println();
                context.out().println("Change detected; rebuilding...");
                ProcessRunner.Result rebuild = ProcessRunner.build(projectDir, List.of("package"), context.out());
                if (rebuild.succeeded()) {
                    server.bumpVersion();
                    context.out().println("Rebuilt successfully; the browser will reload.");
                } else {
                    context.err().println();
                    context.err().println("Rebuild failed; serving the last good build.");
                    for (String line : BuildDiagnostics.diagnose(rebuild.outputTail())) {
                        context.err().println("  " + line);
                    }
                }
            }, context.err());
        } catch (IOException e) {
            server.close();
            throw new CliException("Could not watch the source tree: " + e.getMessage());
        }

        int boundPort = server.port();
        context.out().println();
        context.out().println("DejaView development server running at http://" + host + ":" + boundPort + "/");
        context.out().println("Watching src/ for changes; the browser reloads automatically.");
        context.out().println("Press Ctrl+C to stop.");

        CountDownLatch shutdown = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            context.out().println("Stopping the development server...");
            watcher.close();
            server.close();
            shutdown.countDown();
        }, "dejaview-shutdown"));
        try {
            shutdown.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            watcher.close();
            server.close();
        }
        return 0;
    }

    private static String requireValue(String[] args, int index, String flag) throws CliException {
        if (index >= args.length) {
            throw new CliException("Option " + flag + " requires a value.");
        }
        return args[index];
    }

    private static int parsePort(String value) throws CliException {
        try {
            int port = Integer.parseInt(value.trim());
            if (port < 1 || port > 65535) {
                throw new CliException("Port must be between 1 and 65535, got " + port);
            }
            return port;
        } catch (NumberFormatException e) {
            throw new CliException("Port must be a number, got \"" + value + "\"");
        }
    }
}
