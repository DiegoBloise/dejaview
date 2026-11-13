package io.dejaview.cli;

import java.nio.file.Path;

/** Implements {@code dejaview new <name>}. */
final class NewCommand {

    private NewCommand() {
    }

    static int run(Context context, String[] args) throws CliException {
        String name = null;
        String packageName = null;
        String groupId = null;
        Path directory = null;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--package" -> packageName = requireValue(args, ++i, "--package");
                case "--group-id" -> groupId = requireValue(args, ++i, "--group-id");
                case "--directory" -> directory = Path.of(requireValue(args, ++i, "--directory"));
                default -> {
                    if (arg.startsWith("--package=")) {
                        packageName = arg.substring("--package=".length());
                    } else if (arg.startsWith("--group-id=")) {
                        groupId = arg.substring("--group-id=".length());
                    } else if (arg.startsWith("--directory=")) {
                        directory = Path.of(arg.substring("--directory=".length()));
                    } else if (arg.startsWith("-")) {
                        throw new CliException("Unknown option for new: " + arg + " (see: dejaview help)");
                    } else if (name == null) {
                        name = arg;
                    } else {
                        throw new CliException("Unexpected argument: " + arg);
                    }
                }
            }
        }
        if (name == null) {
            throw new CliException("Missing project name.\nUsage: dejaview new <name> [--package com.example.app] [--group-id <id>] [--directory <dir>]");
        }
        ProjectGenerator.NewProject project = ProjectGenerator.generate(context, name, packageName, groupId, directory);
        context.out().println("Created DejaView project '" + project.artifactId() + "' in " + project.directory());
        context.out().println();
        context.out().println("Next steps:");
        context.out().println("  cd " + project.directory());
        context.out().println("  ./mvnw package");
        context.out().println("  dejaview run");
        context.out().println();
        context.out().println("Or run the app once and serve it with live reload: dejaview run");
        return 0;
    }

    private static String requireValue(String[] args, int index, String flag) throws CliException {
        if (index >= args.length) {
            throw new CliException("Option " + flag + " requires a value.");
        }
        return args[index];
    }
}
