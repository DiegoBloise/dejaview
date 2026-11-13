# __NAME__

A [DejaView](https://github.com/dejaview/dejaview) application: Java 25 written,
compiled to JavaScript by TeaVM, running directly in the browser.

## Commands

```bash
./mvnw package     # compile Java to JavaScript into target/webapp
dejaview run       # build, serve on http://localhost:4321 and reload the browser on change
dejaview doctor    # validate the JDK, the Maven wrapper and the project layout
```

`dejaview` is the DejaView CLI (`io.dejaview:dejaview-cli`). If the command is not on
your `PATH` yet, run it through Maven or the jar directly:

```bash
java -jar path/to/dejaview-cli.jar run
```

## Layout

| Path | Purpose |
| --- | --- |
| `src/main/java/__PACKAGE_PATH__/` | Application components (see `CounterComponent`) |
| `src/main/webapp/index.html` | Host page; mounts the app under `#app` |
| `dejaview.properties` | Dev server settings (`port`, `host`, `webappDir`) |

## Requirements

JDK 25+ and the Maven wrapper (already included). The first build downloads
dependencies from Maven Central.
