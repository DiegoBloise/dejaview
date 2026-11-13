package io.dejaview.cli;

import java.nio.file.Path;

/**
 * Entry point of the {@code dejaview} command. Usage:
 *
 * <pre>
 * dejaview new &lt;name&gt; [--package com.example.app] [--group-id &lt;id&gt;] [--directory &lt;dir&gt;]
 * dejaview build [-- &lt;maven args&gt;]
 * dejaview run [--port N] [--host H] [--no-build]
 * dejaview doctor [--no-port-check]
 * dejaview help
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(execute(args, Path.of("").toAbsolutePath().normalize()));
    }

    /** Testable entry point: returns the exit code instead of exiting. */
    static int execute(String[] args, Path cwd) {
        Context context = Context.system(cwd);
        if (args.length == 0) {
            usage(context, context.out());
            return 2;
        }
        if (isHelp(args[0])) {
            usage(context, context.out());
            return 0;
        }
        String command = args[0];
        String[] rest = java.util.Arrays.copyOfRange(args, 1, args.length);
        try {
            return switch (command) {
                case "new" -> NewCommand.run(context, rest);
                case "build" -> BuildCommand.run(context, rest);
                case "run" -> RunCommand.run(context, rest);
                case "doctor" -> DoctorCommand.run(context, rest);
                case "--version", "-v", "version" -> {
                    context.out().println("dejaview " + Version.current());
                    yield 0;
                }
                default -> {
                    context.err().println("Unknown command: " + command);
                    context.err().println();
                    usage(context, context.err());
                    yield 2;
                }
            };
        } catch (CliException e) {
            context.err().println(e.getMessage());
            return e.exitCode();
        } catch (Exception e) {
            context.err().println("Unexpected error: " + e);
            return 1;
        }
    }

    private static boolean isHelp(String arg) {
        return arg.equals("help") || arg.equals("--help") || arg.equals("-h");
    }

    private static void usage(Context context, java.io.PrintStream stream) {
        stream.println("dejaview " + Version.current() + " — DejaView command line tools");
        stream.println();
        stream.println("Usage:");
        stream.println("  dejaview new <name> [--package com.example.app] [--group-id <id>] [--directory <dir>]");
        stream.println("  dejaview build [-- <maven args>]     Build the project into target/webapp");
        stream.println("  dejaview run [--port N] [--host H] [--no-build]");
        stream.println("                                       Build, serve on http://localhost:4321 and reload on change");
        stream.println("  dejaview doctor [--no-port-check]    Validate the environment and the project layout");
        stream.println("  dejaview help                        Show this help");
        stream.println("  dejaview --version                   Print the version");
        stream.println();
        stream.println("Configuration is read from dejaview.properties at the project root (port, host, webappDir).");
        stream.println("Command-line flags take precedence over the file.");
    }
}
