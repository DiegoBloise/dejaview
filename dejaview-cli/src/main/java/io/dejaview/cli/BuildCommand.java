package io.dejaview.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Implements {@code dejaview build}. Extra arguments after {@code --} go to Maven. */
final class BuildCommand {

    private BuildCommand() {
    }

    static int run(Context context, String[] args) throws CliException {
        Path projectDir = context.cwd();
        ensureProject(projectDir);
        List<String> mavenArgs = new java.util.ArrayList<>();
        mavenArgs.add("package");
        boolean afterSeparator = false;
        for (String arg : args) {
            if (!afterSeparator && arg.equals("--")) {
                afterSeparator = true;
                continue;
            }
            if (afterSeparator) {
                mavenArgs.add(arg);
            } else if (arg.startsWith("-")) {
                throw new CliException("Unknown option for build: " + arg + ". Use 'dejaview build -- <maven args>' to pass arguments to Maven.");
            } else {
                throw new CliException("Unexpected argument for build: " + arg);
            }
        }
        ProcessRunner.Result result = ProcessRunner.build(projectDir, mavenArgs, context.out());
        if (result.succeeded()) {
            context.out().println();
            context.out().println("Build succeeded. Static site in " + projectDir.resolve("target/webapp"));
            return 0;
        }
        printDiagnostics(context, result.outputTail());
        return 1;
    }

    static void ensureProject(Path projectDir) throws CliException {
        if (!Files.exists(projectDir.resolve("pom.xml"))) {
            throw new CliException("No pom.xml in " + projectDir + ". Run 'dejaview doctor' to diagnose.");
        }
        if (!Files.exists(projectDir.resolve("mvnw")) && !Files.exists(projectDir.resolve("mvnw.cmd"))) {
            throw new CliException("No Maven wrapper found in " + projectDir + ". Generated projects embed the wrapper; recreate the project with 'dejaview new'.");
        }
    }

    static void printDiagnostics(Context context, List<String> outputTail) {
        context.err().println();
        context.err().println("Build failed. Diagnostic summary:");
        for (String line : BuildDiagnostics.diagnose(outputTail)) {
            context.err().println("  " + line);
        }
        context.err().println();
        context.err().println("Run 'dejaview doctor' to validate the environment, or 'dejaview build -- -e' for the full stack trace.");
    }
}
