package io.dejaview.compiler;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Build-time verification entry point for the assembled browser bundle.
 *
 * <p>Invoked by the browser modules during the {@code verify} phase through
 * exec-maven-plugin:</p>
 *
 * <pre>
 * &lt;argument&gt;--webapp&lt;/argument&gt;
 * &lt;argument&gt;${project.build.directory}/webapp&lt;/argument&gt;
 * </pre>
 *
 * <p>Behaviour:</p>
 * <ul>
 *   <li>validates the webapp directory (index.html, local references, bundle presence/size);</li>
 *   <li>{@code --no-index} validates a bundle-only output (e.g. the website, whose host pages live in {@code website/});</li>
 *   <li>writes {@code dejaview-assets.json} (paths, bytes, SHA-256) next to the bundle;</li>
 *   <li>prints the validation report;</li>
 *   <li>no errors — returns normally (warnings are printed, build continues);</li>
 *   <li>errors — throws {@link IllegalStateException} carrying the report, which fails the build.</li>
 * </ul>
 *
 * <p>This class is build tooling. It must never be referenced from browser code.</p>
 */
public final class VerifyWebApp {

    public static final String MANIFEST_NAME = AssetManifest.MANIFEST_NAME;

    private VerifyWebApp() {
    }

    public static void main(String[] args) throws IOException {
        Path webapp = null;
        long budgetBytes = WebAppValidator.ValidatorOptions.defaults().sizeBudgetBytes();
        boolean sourceMaps = true;
        boolean requireIndex = true;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--webapp" -> {
                    if (i + 1 >= args.length) {
                        throw usage("--webapp requires a directory argument");
                    }
                    webapp = Path.of(args[++i]);
                }
                case "--budget-bytes" -> {
                    if (i + 1 >= args.length) {
                        throw usage("--budget-bytes requires a number");
                    }
                    try {
                        budgetBytes = Long.parseLong(args[++i].trim());
                    } catch (NumberFormatException e) {
                        throw usage("--budget-bytes must be a number, got: " + args[i]);
                    }
                }
                case "--no-source-maps" -> sourceMaps = false;
                case "--no-index" -> requireIndex = false;
                case "--help" -> throw usage(null);
                default -> throw usage("Unknown option: " + arg);
            }
        }
        if (webapp == null) {
            throw usage("Missing required --webapp <dir>");
        }

        ValidationReport report = WebAppValidator.validate(
                webapp, new WebAppValidator.ValidatorOptions("app.js", budgetBytes, sourceMaps, requireIndex));

        if (report.hasErrors()) {
            System.err.println(report.render());
            throw new IllegalStateException("DejaView webapp verification failed:\n" + report.render());
        }

        AssetManifest manifest = AssetManifest.generate(webapp);
        manifest.write(webapp.resolve(MANIFEST_NAME));

        System.out.println(report.render());
        System.out.println("DejaView asset manifest written to " + manifest.entries().size()
                + " file(s), " + manifest.totalBytes() + " bytes -> " + webapp.resolve(MANIFEST_NAME));
    }

    private static IllegalStateException usage(String message) {
        String text = "Usage: VerifyWebApp --webapp <dir> [--budget-bytes <n>] [--no-source-maps] [--no-index]";
        return new IllegalStateException(message == null ? text : message + System.lineSeparator() + text);
    }
}
