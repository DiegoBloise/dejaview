package io.dejaview.cli;

import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Execution context shared by all commands. Tests construct their own context
 * with a temporary working directory and captured streams; production uses
 * {@link #system(Path)}.
 */
public record Context(Path cwd, PrintStream out, PrintStream err) {

    public static Context system(Path cwd) {
        return new Context(cwd, System.out, System.err);
    }
}
