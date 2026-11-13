package io.dejaview.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Watches the project's source trees and triggers a rebuild after changes go
 * quiet for {@link #DEBOUNCE_MS} ms. Runs on a virtual thread; rebuilds run
 * inline on that thread so builds never overlap.
 */
final class BuildWatcher implements AutoCloseable {

    static final long DEBOUNCE_MS = 300;

    private final WatchService watcher;
    private final Map<WatchKey, Path> keyToDir = new HashMap<>();
    private final Path projectDir;
    private final Runnable rebuild;
    private final PrintStream err;
    private final Thread thread;
    private volatile boolean running = true;

    BuildWatcher(Path projectDir, Runnable rebuild, PrintStream err) throws IOException {
        this.projectDir = projectDir;
        this.rebuild = rebuild;
        this.err = err;
        this.watcher = FileSystems.getDefault().newWatchService();
        for (String rel : List.of("src/main/java", "src/main/webapp", "src/test/java")) {
            registerTree(projectDir.resolve(rel));
        }
        this.thread = Thread.ofVirtual().name("dejaview-watch").start(this::loop);
    }

    private void registerTree(Path root) {
        if (!Files.isDirectory(root)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(root)) {
            for (Path dir : walk.filter(Files::isDirectory).toList()) {
                register(dir);
            }
        } catch (IOException e) {
            err.println("Warning: could not watch " + root + ": " + e.getMessage());
        }
    }

    private void register(Path dir) {
        try {
            WatchKey key = dir.register(watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            keyToDir.put(key, dir);
        } catch (IOException e) {
            err.println("Warning: could not watch " + dir + ": " + e.getMessage());
        }
    }

    private void loop() {
        long pendingSince = -1;
        while (running) {
            WatchKey key = null;
            try {
                if (pendingSince < 0) {
                    key = watcher.take();
                } else {
                    key = watcher.poll(50, TimeUnit.MILLISECONDS);
                }
            } catch (InterruptedException | java.nio.file.ClosedWatchServiceException e) {
                return;
            }
            if (key != null) {
                Path dir = keyToDir.get(key);
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == StandardWatchEventKinds.OVERFLOW) {
                        pendingSince = System.currentTimeMillis();
                        continue;
                    }
                    Path child = (Path) event.context();
                    Path full = dir != null ? dir.resolve(child) : child;
                    if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && Files.isDirectory(full)) {
                        registerTree(full);
                    }
                    pendingSince = System.currentTimeMillis();
                }
                if (!key.reset()) {
                    keyToDir.remove(key);
                }
            }
            if (pendingSince >= 0 && System.currentTimeMillis() - pendingSince >= DEBOUNCE_MS) {
                pendingSince = -1;
                try {
                    rebuild.run();
                } catch (Exception e) {
                    err.println("Rebuild failed unexpectedly: " + e);
                    e.printStackTrace(err);
                }
            }
        }
    }

    @Override
    public void close() {
        running = false;
        thread.interrupt();
        try {
            watcher.close();
        } catch (IOException e) {
            err.println("Warning: could not stop the file watcher: " + e.getMessage());
        }
    }
}
