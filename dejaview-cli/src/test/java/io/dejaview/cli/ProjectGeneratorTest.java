package io.dejaview.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectGeneratorTest {

    @TempDir
    Path tempDir;

    private Context context() {
        return new Context(tempDir,
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
    }

    @Test
    void generates_a_complete_project() throws Exception {
        Path target = tempDir.resolve("generated");
        ProjectGenerator.NewProject project =
                ProjectGenerator.generate(context(), "my-app", null, null, target);

        assertEquals("my-app", project.artifactId());
        assertEquals("com.example.myapp", project.packageName());
        assertEquals("com.example.myapp", project.groupId());
        assertTrue(Files.isRegularFile(target.resolve("pom.xml")));
        assertTrue(Files.isRegularFile(target.resolve("src/main/java/com/example/myapp/App.java")));
        assertTrue(Files.isRegularFile(target.resolve("src/main/java/com/example/myapp/CounterComponent.java")));
        assertTrue(Files.isRegularFile(target.resolve("src/main/webapp/index.html")));
        assertTrue(Files.isRegularFile(target.resolve("dejaview.properties")));
        assertTrue(Files.isRegularFile(target.resolve("README.md")));
        assertTrue(Files.isRegularFile(target.resolve(".gitignore")));
        assertTrue(Files.isRegularFile(target.resolve(".mvn/wrapper/maven-wrapper.properties")));
        assertTrue(Files.isRegularFile(target.resolve("mvnw")));
        assertTrue(Files.isRegularFile(target.resolve("mvnw.cmd")));

        if (target.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            PosixFileAttributes attrs = Files.readAttributes(target.resolve("mvnw"), PosixFileAttributes.class);
            assertTrue(attrs.permissions().contains(
                    PosixFilePermissions.fromString("rwxr-xr-x").iterator().next()));
        }
    }

    @Test
    void substitutes_all_placeholders() throws Exception {
        Path target = tempDir.resolve("substituted");
        ProjectGenerator.generate(context(), "my-app", "org.foo.bar", "org.foo", target);

        String pom = Files.readString(target.resolve("pom.xml"));
        String index = Files.readString(target.resolve("src/main/webapp/index.html"));
        String app = Files.readString(target.resolve("src/main/java/org/foo/bar/App.java"));
        String readme = Files.readString(target.resolve("README.md"));

        assertFalse(pom.contains("__"), "pom.xml still contains placeholders");
        assertFalse(index.contains("__"), "index.html still contains placeholders");
        assertFalse(app.contains("__"), "App.java still contains placeholders");
        assertFalse(readme.contains("__"), "README.md still contains placeholders");

        assertTrue(pom.contains("<artifactId>my-app</artifactId>"));
        assertTrue(pom.contains("<groupId>org.foo</groupId>"));
        assertTrue(pom.contains("<mainClass>org.foo.bar.App</mainClass>"));
        assertTrue(app.contains("package org.foo.bar;"));
        assertTrue(index.contains("my-app"));
        assertTrue(readme.contains("org/foo/bar/"));
    }

    @Test
    void refuses_to_overwrite_a_non_empty_directory() throws Exception {
        Path target = tempDir.resolve("occupied");
        Files.createDirectories(target);
        Files.writeString(target.resolve("keep.txt"), "precious");

        assertThrows(CliException.class,
                () -> ProjectGenerator.generate(context(), "my-app", null, null, target));
        assertEquals("precious", Files.readString(target.resolve("keep.txt")));
    }

    @Test
    void rejects_invalid_names_and_packages() {
        assertThrows(CliException.class,
                () -> ProjectGenerator.generate(context(), "my app", null, null, tempDir.resolve("a")));
        assertThrows(CliException.class,
                () -> ProjectGenerator.generate(context(), "", null, null, tempDir.resolve("b")));
        assertThrows(CliException.class,
                () -> ProjectGenerator.generate(context(), "ok", "Foo-Bar", null, tempDir.resolve("c")));
    }

    @Test
    void sanitizes_artifact_ids() {
        assertEquals("my-app", ProjectGenerator.sanitizeArtifactId("my-app"));
        assertEquals("my-app", ProjectGenerator.sanitizeArtifactId("My_APP"));
        assertEquals("app", ProjectGenerator.sanitizeArtifactId("---"));
        assertEquals("com.example.myapp", ProjectGenerator.defaultPackage("my-app"));
    }
}
