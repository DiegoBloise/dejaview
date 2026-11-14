package io.dejaview.examples.ssg;

import io.dejaview.ssg.SsgGenerator;
import io.dejaview.ssg.SsgResult;

import java.nio.file.Path;

/**
 * Generates the example site.
 *
 * <p>Run with {@code ./mvnw -pl examples/ssg -am package}; the module binds this
 * class to the {@code package} phase through {@code exec-maven-plugin} and
 * passes the output directory and the assets directory, both anchored to the
 * module basedir. When run by hand, pass them as the first and second arguments:
 * {@code SsgDemoMain <outputDir> <assetsDir>}</p>
 */
public final class SsgDemoMain {

    private SsgDemoMain() {
    }

    public static void main(String[] args) {
        Path output = Path.of(args.length > 0 ? args[0] : "target/site");
        Path assets = args.length > 1 ? Path.of(args[1]) : Path.of("src/main/webapp/assets");
        SsgResult result = SsgGenerator.generate(StaticSite.site(output, assets));
        System.out.println("Static site generated: " + result.pageCount() + " page(s), "
                + result.assetCount() + " asset(s) in " + result.outputDir());
        result.sitemapFile().ifPresent(sitemap -> System.out.println("Sitemap: " + sitemap));
    }
}
