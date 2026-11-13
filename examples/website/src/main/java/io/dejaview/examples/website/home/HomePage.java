package io.dejaview.examples.website.home;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.examples.website.Lang;
import io.dejaview.examples.website.component.CodeBlock;
import io.dejaview.examples.website.component.Icons;
import io.dejaview.examples.website.component.SiteChrome;
import io.dejaview.reactivity.Signal;

/**
 * Landing page, rendered by DejaView. Copy exists in EN and PT side by side;
 * the static shell selects the locale through {@code window.DEJAVIEW_LANG}.
 */
public final class HomePage extends AbstractComponent {

    private static final String COUNTER_SNIPPET = """
            public final class Counter extends AbstractComponent {
                private final Signal<Integer> count = new Signal<>(0);

                @Override
                public View render() {
                    return Views.div(
                        Views.h1("Counter"),
                        Views.p("Count: " + count.get()).id("count"),
                        Views.button("Increment")
                            .onClick(() -> count.update(v -> v + 1))
                    ).classes("example");
                }
            }""";

    private static final String CLONE_BUILD = """
            git clone https://github.com/dejaview/dejaview.git
            cd dejaview
            ./mvnw clean verify""";

    private static final String SERVE_COUNTER = "python3 -m http.server 4280 -d examples/counter/target/webapp";

    private final Lang lang;
    private final Signal<String> demoTab = new Signal<>("counter");

    public HomePage(Lang lang) {
        this.lang = lang;
    }

    private String t(String en, String pt) {
        return lang.t(en, pt);
    }

    @Override
    public View render() {
        return SiteChrome.page(lang, "home", Views.fragment(
                hero(),
                features(),
                demo(),
                philosophy(),
                gettingStarted(),
                ctaBand()));
    }

    private View hero() {
        return Views.element("section",
                        Views.div(
                                Views.div(
                                        Views.p(
                                                Views.element("span").classes("dot").attr("aria-hidden", "true"),
                                                Views.text(" " + t("Java-first frontend framework", "Framework frontend Java")))
                                                .classes("eyebrow"),
                                        Views.element("h1",
                                                Views.text("Java."),
                                                Views.element("br"),
                                                Views.element("span", Views.text(t(
                                                        "Reimagined for the Web.",
                                                        "Reimaginado para a Web."))).classes("gradient-text"))
                                                .id("hero-title"),
                                        Views.p(t(
                                                "Build reactive web interfaces in pure Java 25 — components, signals and a reactive renderer, compiled to plain JavaScript you can deploy anywhere.",
                                                "Crie interfaces web reativas em Java puro — componentes, signals e um renderer reativo, compilados para JavaScript simples que você pode publicar em qualquer lugar."))
                                                .classes("hero-sub"),
                                        Views.div(
                                                link(lang.playgroundUrl(), "btn btn-primary", t("Try the playground", "Experimente o playground")),
                                                link(lang.getStartedUrl(), "btn btn-ghost", t("Get started →", "Primeiros passos →")))
                                                .classes("hero-actions"),
                                        Views.element("ul",
                                                        Views.li(t("Signals & computed", "Signals e computed")),
                                                        Views.li(t("Components, inputs & slots", "Componentes, inputs e slots")),
                                                        Views.li(t("Router & HTTP client", "Rotas e cliente HTTP")))
                                                .classes("hero-proof")
                                                .attr("aria-label", t("What DejaView gives you", "O que o DejaView oferece")))
                                        .classes("hero-copy"),
                                Views.element("figure",
                                        Views.div(
                                                Views.div(
                                                        Views.element("span").classes("win-dot"),
                                                        Views.element("span").classes("win-dot"),
                                                        Views.element("span").classes("win-dot"),
                                                        Views.span("Counter.java").classes("code-window-title"))
                                                        .classes("code-window-bar")
                                                        .attr("aria-hidden", "true"),
                                                Views.element("pre",
                                                        Views.element("code", Views.text(COUNTER_SNIPPET))
                                                                .attr("data-highlight-java", "")))
                                                .classes("code-window"),
                                        Views.element("figcaption",
                                                Views.text(t("A real DejaView component — it runs in the demo below.",
                                                             "Um componente DejaView real — ele roda na demo abaixo."))))
                                        .classes("hero-code")
                                        .attr("aria-label", t("Example DejaView component", "Exemplo de componente DejaView")))
                                .classes("container", "hero-inner"))
                .classes("hero")
                .attr("aria-labelledby", "hero-title");
    }

    private View features() {
        return Views.element("section",
                        Views.div(
                                Views.p(t("Why DejaView", "Por que DejaView")).classes("eyebrow"),
                                Views.element("h2", Views.text(t(
                                        "Everything you need to build for the web — in Java",
                                        "Tudo que você precisa para a web — em Java"))).id("features-title"))
                                .classes("section-head"),
                        Views.div(
                                feature(Icons.code(), t("Java in, JavaScript out", "Java dentro, JavaScript fora"),
                                        t("Write your app in Java 25 and ship plain JavaScript. No WebAssembly, no proprietary browser runtime — just your code as a static asset.",
                                          "Escreva seu app em Java 25 e publique JavaScript simples. Sem WebAssembly, sem runtime proprietário no navegador — seu código como um asset estático.")),
                                feature(Icons.pulse(), t("Signals & computed", "Signals e computed"),
                                        t("Fine-grained reactivity with Signal, Effect and lazy Computed. Only what actually changed re-renders.",
                                          "Reatividade granular com Signal, Effect e Computed preguiçoso. Só o que realmente mudou re-renderiza.")),
                                feature(Icons.monitor(), t("Reactive renderer", "Renderer reativo"),
                                        t("A declarative view tree with reconciliation — DOM nodes are reused and updated surgically on every signal change.",
                                          "Uma árvore de views declarativa com reconciliação — os nós do DOM são reutilizados e atualizados de forma cirúrgica a cada mudança de signal.")),
                                feature(Icons.hexagon(), t("Components, inputs & slots", "Componentes, inputs e slots"),
                                        t("Class-based or functional components with typed inputs, outputs, named slots and lifecycle hooks — familiar if you know Angular or Vue.",
                                          "Componentes por classe ou funcionais com inputs tipados, outputs, slots nomeados e hooks de ciclo de vida — familiar para quem conhece Angular ou Vue.")),
                                feature(Icons.route(), t("Client-side routing", "Rotas no cliente"),
                                        t("Nested routes, :param patterns, guards, redirects and browser history — wired to your components through RouterOutlet.",
                                          "Rotas aninhadas, padrões :param, guards, redirects e histórico do navegador — conectados aos seus componentes pelo RouterOutlet.")),
                                feature(Icons.download(), t("HTTP client", "Cliente HTTP"),
                                        t("A typed HTTP client with interceptors, timeouts, cancellation and dependency-free JSON handling — ready for real APIs.",
                                          "Um cliente HTTP tipado com interceptors, timeouts, cancelamento e tratamento de JSON sem dependências — pronto para APIs reais.")),
                                feature(Icons.plug(), t("Dependency injection", "Injeção de dependência"),
                                        t("Explicit providers, singleton/scoped/transient lifetimes and hierarchical injectors. No reflection, no hidden magic.",
                                          "Providers explícitos, escopos singleton/scoped/transient e injetores hierárquicos. Sem reflection, sem mágica escondida.")),
                                feature(Icons.lines(), t("Boring HTML & CSS", "HTML e CSS monótonos"),
                                        t("Plain class and style attributes, untouched by the framework. Bootstrap, Tailwind CSS and daisyUI just work.",
                                          "Atributos class e style simples, intactos pelo framework. Bootstrap, Tailwind CSS e daisyUI funcionam de primeira.")),
                                feature(Icons.check(), t("Type-safe by design", "Tipagem segura por design"),
                                        t("The API is plain records, interfaces and generics on Java 25. Your IDE and the compiler catch mistakes before the browser does.",
                                          "A API são records, interfaces e generics de Java puro. Sua IDE e o compilador pegam erros antes do navegador.")))
                                .classes("card-grid"))
                .id("features")
                .classes("section", "container")
                .attr("aria-labelledby", "features-title");
    }

    private View feature(View icon, String title, String description) {
        return Views.element("article",
                        Views.div(icon).classes("card-icon").attr("aria-hidden", "true"),
                        Views.element("h3", Views.text(title)),
                        Views.p(description))
                .classes("card");
    }

    private View link(String href, String classes, String label) {
        return Views.element("a", Views.text(label)).attr("href", href).classes(classes.split(" "));
    }

    private View demo() {
        String selected = demoTab.get();
        return Views.element("section",
                        Views.div(
                                Views.p(t("Live demo", "Demo ao vivo")).classes("eyebrow"),
                                Views.element("h2", Views.text(t(
                                        "Real DejaView, running right now",
                                        "DejaView de verdade, rodando agora"))).id("demo-title"),
                                Views.p(t(
                                                "Not a mock-up — the actual framework, compiled from Java and executing your clicks in this page.",
                                                "Isto não é uma maquete — é o framework de verdade, compilado de Java e executando seus cliques nesta página."))
                                        .classes("section-sub"))
                                .classes("section-head"),
                        Views.div(
                                Views.div(
                                        Views.div(
                                                demoTab("counter", t("Counter", "Contador"), selected),
                                                demoTab("list", t("Keyed list", "Lista com chaves"), selected),
                                                demoTab("router", t("Router", "Rotas"), selected),
                                                demoTab("http", "HTTP", selected))
                                                .classes("demo-tabs")
                                                .attr("role", "group")
                                                .attr("aria-label", t("Choose an example", "Escolha um exemplo")),
                                        Views.p(demoCaption(selected)).id("demo-caption").classes("demo-caption"),
                                        Views.p(Views.text(t(
                                                        "Every example is written in Java, compiled to JavaScript, and is the same code you see in the ",
                                                        "Todo exemplo é escrito em Java, compilado para JavaScript, e é o mesmo código que você vê no ")),
                                                Views.element("a", Views.text("playground")).attr("href", lang.playgroundUrl()),
                                                Views.text("."))
                                                .classes("demo-note"),
                                        Views.p(link(lang.playgroundUrl(), "btn btn-ghost",
                                                t("Open the playground →", "Abrir o playground →"))))
                                        .classes("demo-copy"),
                                Views.div(
                                        Views.element("iframe")
                                                .id("demo-frame")
                                                .attr("src", "/examples/showcase/target/webapp/index.html?example=" + selected)
                                                .attr("title", t("Live DejaView example", "Exemplo DejaView ao vivo"))
                                                .attr("loading", "lazy"))
                                        .classes("demo-frame"))
                                .classes("demo-grid"))
                .id("demo")
                .classes("section", "container")
                .attr("aria-labelledby", "demo-title");
    }

    private View demoTab(String id, String label, String selected) {
        boolean active = id.equals(selected);
        return Views.button(label)
                .classes("demo-tab")
                .attr("id", "tab-" + id)
                .attr("aria-pressed", active ? "true" : "false")
                .attr("aria-controls", "demo-frame")
                .onClick(() -> demoTab.set(id));
    }

    private String demoCaption(String id) {
        return switch (id) {
            case "list" -> t("Keyed reconciliation keeps DOM nodes stable while a list is reordered.",
                             "A reconciliação com chaves mantém os nós do DOM estáveis quando a lista é reordenada.");
            case "router" -> t("Client-side routes with parameters, outlets and browser history.",
                               "Rotas no cliente com parâmetros, outlets e histórico do navegador.");
            case "http" -> t("A typed HTTP call, a signal, and a reactive update when the response lands.",
                             "Uma chamada HTTP tipada, um signal e uma atualização reativa quando a resposta chega.");
            default -> t("One signal, one component, one button — the smallest reactive DejaView app.",
                         "Um signal, um componente, um botão — o menor app reativo em DejaView.");
        };
    }

    private View philosophy() {
        return Views.element("section",
                        Views.div(
                                Views.p(t("Our approach", "Nossa abordagem")).classes("eyebrow"),
                                Views.element("h2", Views.text(t(
                                        "Three promises, kept in code",
                                        "Três promessas, cumpridas em código"))).id("philosophy-title"))
                                .classes("section-head"),
                        Views.div(
                                promise(t("Java stays primary", "Java acima de tudo"),
                                        t("No DSL, no annotation processors required to start. The API is plain records, interfaces and generics — the Java you already know.",
                                          "Sem DSL, sem annotation processors para começar. A API são records, interfaces e generics puros — o Java que você já conhece.")),
                                promise(t("Reactive by design", "Reativo por design"),
                                        t("State lives in signals, and the renderer tracks exactly what each view reads. You never write manual DOM updates.",
                                          "O estado vive em signals e o renderer rastreia exatamente o que cada view lê. Você nunca escreve atualizações manuais de DOM.")),
                                promise(t("No lock-in", "Sem amarras"),
                                        t("Your app compiles to static HTML and JavaScript. Bring your own CSS framework, your own hosting, your own pace.",
                                          "Seu app compila para HTML e JavaScript estáticos. Traga seu próprio framework de CSS, sua própria hospedagem, seu próprio ritmo.")))
                                .classes("promise-grid"))
                .id("philosophy")
                .classes("section", "container")
                .attr("aria-labelledby", "philosophy-title");
    }

    private View promise(String title, String description) {
        return Views.div(Views.element("h3", Views.text(title)), Views.p(description))
                .classes("promise");
    }

    private View gettingStarted() {
        return Views.element("section",
                        Views.div(
                                Views.p(t("Get started", "Primeiros passos")).classes("eyebrow"),
                                Views.element("h2", Views.text(t(
                                        "From zero to a running counter in minutes",
                                        "Do zero a um contador rodando em minutos"))).id("gs-title"))
                                .classes("section-head"),
                        Views.element("ol",
                                step("1", t("Prerequisites", "Pré-requisitos"),
                                        Views.p(t("JDK 25 and Git. Maven itself is not required — the repository ships the Maven Wrapper.",
                                                   "JDK 25 e Git. O Maven em si não é necessário — o repositório já inclui o Maven Wrapper."))),
                                step("2", t("Build it", "Compile"),
                                        Views.componentFactory(() -> new CodeBlock(lang, CLONE_BUILD, false))),
                                step("3", t("Run it", "Rode"),
                                        Views.fragment(
                                                Views.p(t("Serve the compiled counter over HTTP and open it in your browser.",
                                                           "Sirva o contador compilado via HTTP e abra no seu navegador.")),
                                                Views.componentFactory(() -> new CodeBlock(lang, SERVE_COUNTER, false)))),
                                step("→", t("Keep going", "Continue"),
                                        Views.fragment(
                                                Views.p(t("The full step-by-step tutorial walks you through components, signals, composition, routing, DI and HTTP.",
                                                           "O tutorial completo guia você por componentes, signals, composição, rotas, DI e HTTP.")),
                                                link(lang.getStartedUrl(), "btn btn-primary", t("Read the complete guide", "Leia o guia completo")))))
                                .classes("gs-steps"))
                .id("getting-started")
                .classes("section", "container")
                .attr("aria-labelledby", "gs-title");
    }

    private View step(String number, String title, View body) {
        return Views.element("li",
                        Views.element("h3",
                                        Views.element("span", Views.text(number)).classes("gs-num"),
                                        Views.text(title)),
                        body)
                .classes("gs-step");
    }

    private View ctaBand() {
        return Views.element("section",
                        Views.element("h2", Views.text(t(
                                "Ready to build with Java on the web?",
                                "Pronto para construir com Java na web?"))).id("cta-title"),
                        Views.p(t("Your first DejaView app is one component away.",
                                   "Seu primeiro app DejaView está a um componente de distância.")),
                        Views.div(
                                link(lang.getStartedUrl(), "btn btn-primary", t("Get started", "Primeiros passos")),
                                link(lang.playgroundUrl(), "btn btn-ghost", t("Open the playground", "Abrir o playground")))
                                .classes("hero-actions", "cta-actions"))
                .classes("section", "container", "cta-band")
                .attr("aria-labelledby", "cta-title");
    }
}
