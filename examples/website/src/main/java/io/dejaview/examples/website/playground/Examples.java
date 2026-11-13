package io.dejaview.examples.website.playground;

import io.dejaview.examples.website.Lang;

import java.util.List;

/**
 * The examples shown in the playground. Every entry compiles into the
 * showcase bundle ({@code ?example=<id>}), so what the page shows is exactly
 * what runs. Sources mirror {@code examples/showcase/ShowcaseApp.java}.
 */
public final class Examples {

    public record Example(String id,
                          String nameEn, String namePt,
                          String descEn, String descPt,
                          String source) {

        public String name(Lang lang) {
            return lang.isPt() ? namePt : nameEn;
        }

        public String desc(Lang lang) {
            return lang.isPt() ? descPt : descEn;
        }
    }

    public static final List<Example> ALL = List.of(

            new Example("counter",
                    "Counter", "Contador",
                    "Class-based component with an incrementable signal",
                    "Componente por classe com um signal incrementável",
                    """
                    // examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java
                    // The same component shape as examples/counter.

                    private static final class CounterExample extends AbstractComponent {

                        private final Signal<Integer> count = new Signal<>(0);

                        @Override
                        public View render() {
                            int current = count.get();
                            return Views.div(
                                            Views.h1("Counter"),
                                            Views.p("Count: " + current)
                                                    .id("count")
                                                    .attr("data-count", Integer.toString(current)),
                                            Views.button("Increment")
                                                    .onClick(() -> count.update(v -> v + 1)))
                                    .classes("example");
                        }
                    }"""),

            new Example("greeting",
                    "Greeting", "Saudação",
                    "Functional component — state captured in the supplier closure",
                    "Componente funcional — o estado fica capturado no closure do fornecedor",
                    """
                    // Functional components: the supplier is re-invoked on every render;
                    // state captured in the closure survives across renders.

                    public static void mountGreeting(String hostId) {
                        Signal<String> name = new Signal<>("");
                        DejaView.mount(Views.component(() ->
                                Views.div(
                                        Views.h1("Hello, " + name.get()),
                                        Views.button("Set name")
                                                .onClick(() -> name.set("DejaView")))
                                        .classes("example")),
                                hostId);
                    }"""),

            new Example("composition",
                    "Component composition", "Composição de componentes",
                    "Parent renders two children with isolated state",
                    "O pai renderiza dois filhos com estado isolado",
                    """
                    // examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java

                    private static final class ParentComponent extends AbstractComponent {

                        @Override
                        public View render() {
                            return Views.div(
                                    Views.h1("Composition"),
                                    Views.componentFactory(ChildComponent::new),
                                    Views.componentFactory(ChildComponent::new));
                        }
                    }

                    private static final class ChildComponent extends AbstractComponent {

                        private final Signal<Integer> clicks = new Signal<>(0);

                        @Override
                        public View render() {
                            return Views.button("Clicks: " + clicks.get())
                                    .onClick(() -> clicks.update(v -> v + 1));
                        }
                    }"""),

            new Example("conditional",
                    "Conditional rendering", "Renderização condicional",
                    "Fragments and ternaries inside a single component",
                    "Fragments e ternários dentro de um único componente",
                    """
                    // examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java

                    private static final class ConditionalComponent extends AbstractComponent {

                        private final Signal<Boolean> show = new Signal<>(true);

                        @Override
                        public View render() {
                            return Views.div(
                                    Views.h1("Conditional"),
                                    Views.button("Toggle")
                                            .onClick(() -> show.update(v -> !v)),
                                    show.get()
                                            ? Views.p("Visible")
                                            : Views.fragment(Views.p("Hidden")));
                        }
                    }"""),

            new Example("usercard",
                    "Inputs, outputs & slots", "Inputs, outputs e slots",
                    "Parent pushes inputs, listens to outputs, projects named slots",
                    "O pai empurra inputs, ouve outputs e projeta slots nomeados",
                    """
                    // examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java

                    private static final class UserCardDemo extends AbstractComponent {

                        private final Signal<String> member = new Signal<>("Ada");
                        private final Signal<String> lastEvent = new Signal<>("—");

                        @Override
                        public View render() {
                            String name = member.get();
                            boolean ada = name.equals("Ada");
                            return Views.div(
                                    Views.h1("Team directory"),
                                    Views.button("Switch member")
                                            .onClick(() -> member.set(ada ? "Grace" : "Ada")),
                                    Views.componentFactory(UserCard.class, UserCard::new)
                                            .input("username", name)
                                            .on("select", selected -> lastEvent.set("Selected " + selected))
                                            .slot("header", Views.strong(ada ? "Core team" : "Guest"))
                                            .slot(Views.p(ada ? "Writes the renderer." : "Reviews every PR.")),
                                    Views.p("Event: " + lastEvent.get()).id("event-log"));
                        }
                    }

                    private static final class UserCard extends AbstractComponent {

                        @Override
                        public void onInit() {
                            // Invoked once per mounted node, before the first render.
                        }

                        @Override
                        public View render() {
                            String username = input("username", String.class);
                            View bio = slot().isEmpty() ? Views.p("No bio yet.") : slot().get(0);
                            return Views.div(
                                            Views.element("header")
                                                    .addChildren(slot("header").toArray(View[]::new)),
                                            Views.h3("@" + username),
                                            bio,
                                            Views.button("Select " + username)
                                                    .onClick(() -> emit("select", username)))
                                    .classes("usercard");
                        }
                    }"""),

            new Example("list",
                    "Keyed list", "Lista com chaves",
                    "Keyed reconciliation keeps DOM nodes stable across reorders",
                    "A reconciliação com chaves mantém os nós do DOM estáveis em reordenações",
                    """
                    // examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java

                    private static final class KeyedListDemo extends AbstractComponent {

                        private final Signal<List<String>> order =
                                new Signal<>(List.of("Alpha", "Beta", "Gamma"));

                        @Override
                        public View render() {
                            List<View> rows = new ArrayList<>();
                            for (String label : order.get()) {
                                rows.add(Views.li(label).key(label));
                            }
                            return Views.div(
                                    Views.h1("Keyed list"),
                                    Views.button("Rotate").onClick(this::rotate),
                                    Views.ul(rows.toArray(View[]::new)));
                        }

                        private void rotate() {
                            List<String> current = new ArrayList<>(order.get());
                            current.add(0, current.remove(current.size() - 1));
                            order.set(List.copyOf(current));
                        }
                    }"""),

            new Example("router",
                    "Routing", "Rotas",
                    "Client-side routes with parameters, history and outlets",
                    "Rotas no cliente com parâmetros, histórico e outlets",
                    """
                    // examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java

                    /** Nested routing with browser history, parameters and back/forward. */
                    public static void mountRouter(String hostId) {
                        AtomicReference<Router> ref = new AtomicReference<>();
                        Router router = Router.builder()
                                .routes(
                                        Route.path("/users/:id")
                                                .component(() -> new RouterUserScreen(ref.get()))
                                                .metadata("screen", "user")
                                                .build(),
                                        Route.path("/about")
                                                .component(() -> new RouterAboutScreen(ref.get()))
                                                .metadata("screen", "about")
                                                .build(),
                                        // Catch-all home: the example also works when served from
                                        // an arbitrary static path such as /index.html.
                                        Route.path("**")
                                                .component(() -> new RouterHomeScreen(ref.get()))
                                                .metadata("screen", "home")
                                                .build())
                                .history(RouterHistory.browser())
                                .build();
                        ref.set(router);
                        DejaView.mount(Views.componentFactory(() -> new RouterShell(router)), hostId);
                    }

                    private static final class RouterShell extends AbstractComponent {

                        private final Router router;

                        RouterShell(Router router) {
                            this.router = router;
                        }

                        @Override
                        public View render() {
                            return Views.div(
                                    Views.h1("Router"),
                                    Views.componentFactory(() -> new RouterOutlet(router)))
                                    .classes("example");
                        }
                    }

                    /** Parameterized screen; the outlet pushes {@code id} as an input. */
                    private static final class RouterUserScreen extends AbstractComponent {

                        private final Router router;

                        RouterUserScreen(Router router) {
                            this.router = router;
                        }

                        @Override
                        public View render() {
                            String id = input("id", String.class);
                            return Views.div(
                                    Views.h2("User profile").id("router-user"),
                                    Views.p("User id: " + id).id("router-user-id"),
                                    Views.button("Home")
                                            .id("router-home-btn")
                                            .onClick(() -> router.navigate("/")),
                                    Views.button("About")
                                            .id("router-user-about")
                                            .onClick(() -> router.navigate("/about")));
                        }
                    }"""),

            new Example("http",
                    "HTTP client", "Cliente HTTP",
                    "Fetches a static resource through HttpClient and renders the body",
                    "Busca um recurso estático pelo HttpClient e renderiza o corpo",
                    """
                    // examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java

                    /** HTTP client demo: button triggers GET ping.json; result is rendered reactively. */
                    private static final class HttpPingDemo extends AbstractComponent {

                        private final Signal<String> result = new Signal<>("—");
                        private final HttpClient client = HttpClient.builder().build();

                        @Override
                        public View render() {
                            return Views.div(
                                            Views.h1("HTTP"),
                                            Views.button("GET ping.json")
                                                    .id("http-ping")
                                                    .onClick(this::ping),
                                            Views.p(result.get()).id("http-result"))
                                    .classes("example");
                        }

                        private void ping() {
                            client.get("ping.json", this::renderOutcome);
                        }

                        private void renderOutcome(HttpResult outcome) {
                            if (outcome instanceof HttpResult.Success success) {
                                result.set(success.response().body());
                            } else {
                                result.set("error: " + outcome.failure().code());
                            }
                        }
                    }"""),

            new Example("forms",
                    "Reactive forms", "Formulários reativos",
                    "Validated controls, async checks, dynamic arrays and submission",
                    "Controles validados, checagens assíncronas, arrays dinâmicos e submissão",
                    """
                    // examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java

                    private static final class FormsDemo extends AbstractComponent {

                        private final FormControl<String> name =
                                new FormControl<>("", Validators.required(), Validators.minLength(2));
                        private final FormControl<String> username =
                                new FormControl<>("", Validators.required(), Validators.minLength(3));
                        private final FormControl<Boolean> newsletter = new FormControl<>(false);
                        private final FormArray tags = new FormArray()
                                .add(new FormControl<>("", Validators.required()));
                        private final FormGroup form = new FormGroup();
                        private final Signal<String> message = new Signal<>("No submission yet.");

                        FormsDemo() {
                            username.addAsyncValidator(FormsDemo::checkUsername);
                            form.addControl("name", name)
                                    .addControl("username", username)
                                    .addControl("newsletter", newsletter)
                                    .addControl("tags", tags);
                        }

                        @Override
                        public View render() {
                            return Views.div(
                                    Views.h1("Reactive forms"),
                                    Views.p("Status: " + form.status()).id("forms-status"),
                                    FormBindings.text("forms-name", name),
                                    FormBindings.text("forms-username", username),
                                    FormBindings.checkbox("forms-newsletter", newsletter),
                                    tagRows(),
                                    Views.button("Add tag")
                                            .id("forms-add-tag")
                                            .attr("type", "button")
                                            .onClick(this::addTag),
                                    Views.button("Submit")
                                            .id("forms-submit")
                                            .attr("type", "button")
                                            .onClick(this::submit),
                                    Views.p(message.get()).id("forms-result"));
                        }

                        private View tagRows() {
                            List<View> rows = new ArrayList<>();
                            for (AbstractControl<?> tag : tags.controls()) {
                                rows.add(FormBindings.text("forms-tag-" + rows.size(),
                                        (FormControl<String>) tag));
                            }
                            return Views.div(rows.toArray(View[]::new));
                        }

                        private void addTag() {
                            tags.add(new FormControl<>("", Validators.required()));
                        }

                        private void submit() {
                            FormSubmission<Map<String, Object>> submission = form.submit();
                            message.set(submission.valid()
                                    ? "Submitted: " + submission.value()
                                    : "Please fix the highlighted fields.");
                        }

                        private static void checkUsername(String value, ValidationCallback callback) {
                            // Simulates a remote availability check.
                            Window.setTimeout(
                                    () -> callback.complete(value.equals("admin")
                                            ? ValidationErrors.of("taken")
                                            : ValidationErrors.none()),
                                    600);
                        }
                    }"""),

            new Example("state",
                    "State management", "Gerenciamento de estado",
                    "Shared stores, derived projections, cross-store combine, async checkout and a persisted theme",
                    "Stores compartilhados, projeções derivadas, combine entre stores, checkout assíncrono e um tema persistido",
                    """
                    // examples/showcase/src/main/java/io/dejaview/examples/showcase/ShowcaseApp.java

                    private static final class StateDemo extends AbstractComponent {

                        private final StateStore<List<String>> cart = new StateStore<>(List.of());
                        private final Computed<Integer> itemCount = cart.select(List::size);
                        private final Computed<Integer> subtotalCents =
                                cart.select(StateDemo::subtotalOf);
                        private final StateStore<Integer> discountPercent = new StateStore<>(0);
                        private final Computed<Integer> totalCents = Stores.combine(
                                cart, discountPercent,
                                (items, percent) -> applyDiscount(subtotalOf(items), percent));
                        private final AsyncStore<String> checkout = new AsyncStore<>(this::runCheckout);
                        private final PersistedStore<String> theme = new PersistedStore<>(
                                "dejaview-demo-theme", new StateStore<>("dark"),
                                BrowserStorage.local(), StateCodec.string());

                        @Override
                        public View render() {
                            AsyncState<String> order = checkout.state();
                            return Views.div(
                                    Views.h1("State management"),
                                    Views.p("Items: " + itemCount.get()).id("state-count"),
                                    Views.p("Subtotal: " + formatCents(subtotalCents.get()))
                                            .id("state-subtotal"),
                                    Views.p("Total: " + formatCents(totalCents.get()))
                                            .id("state-total"),
                                    Views.button("Add book (12.00)").onClick(() -> addItem("book")),
                                    Views.button("10% off").id("state-discount-10")
                                            .onClick(() -> discountPercent.set(10)),
                                    Views.button("Checkout").id("state-checkout")
                                            .onClick(checkout::load),
                                    Views.p(resultText(order)).id("state-result")
                                            .attr("role", "status"),
                                    Views.p("Theme: " + theme.store().state()).id("state-theme"),
                                    Views.button("Toggle theme").id("state-theme-toggle")
                                            .onClick(() ->
                                                    theme.store().update(
                                                            v -> "dark".equals(v) ? "light" : "dark")));
                        }

                        private void runCheckout(Consumer<String> succeed,
                                                 Consumer<AsyncError> fail) {
                            if (cart.state().isEmpty()) {
                                fail.accept(AsyncError.of("empty-cart", "cannot checkout"));
                                return;
                            }
                            Window.setTimeout(() -> succeed.accept("order-1"), 700);
                        }

                        private String resultText(AsyncState<String> order) {
                            if (order.isLoading()) {
                                return "Placing your order…";
                            }
                            if (order.isSuccess()) {
                                return "Order placed: " + order.data().orElse("");
                            }
                            if (order.isFailure()) {
                                return "Checkout failed: " + order.error().map(AsyncError::code).orElse("unknown");
                            }
                            return "No checkout attempted yet.";
                        }
                    }""")
    );

    private Examples() {
    }

    public static Example byId(String id) {
        for (Example example : ALL) {
            if (example.id().equals(id)) {
                return example;
            }
        }
        return ALL.get(0);
    }
}
