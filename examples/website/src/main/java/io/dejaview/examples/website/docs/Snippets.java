package io.dejaview.examples.website.docs;

/**
 * Java snippets shared by the EN and PT versions of the tutorial. Every
 * snippet mirrors code that exists in {@code examples/counter} or
 * {@code examples/showcase}, so what the docs show is what actually compiles.
 */
public final class Snippets {

    private Snippets() {
    }

    public static final String REQUIREMENTS = """
            java -version""";

    public static final String CLONE_BUILD = """
            git clone https://github.com/dejaview/dejaview.git
            cd dejaview
            ./mvnw clean verify""";

    public static final String WEBAPP_LAYOUT = """
            examples/counter/target/webapp/
            ├── app.js
            ├── app.js.map
            ├── dejaview-assets.json
            └── index.html""";

    public static final String PRODUCTION_BUILD = """
            # production pipeline: FULL dead-code elimination,
            # minified output, 768 KiB output tripwire
            ./mvnw clean package -Pproduction""";

    public static final String SERVER_PLATFORM = """
            InjectorBuilder builder = Injector.builder().name("server");
            ServerBindings.apply(builder);

            try (Injector root = builder.build();
                 ServerContext context = ServerContext.create(
                         root, ServerRequest.of("GET", "/"))) {

                ServerTree tree = ServerRenderer.render(
                        Views.p("Hello from the server"), context);
            }""";

    public static final String SSR_PAGE = """
            final class ProductsPage extends AbstractComponent {
                private final PageMeta meta;
                private final List<Product> products;

                ProductsPage(PageMeta meta, List<Product> products) {
                    this.meta = meta;
                    this.products = products;
                }

                @Override
                public View render() {
                    meta.title("Products")
                            .description(products.size() + " product(s) found.");
                    View[] items = products.stream()
                            .map(product -> Views.li(product.name()))
                            .toArray(View[]::new);
                    return Views.div(
                            Views.h1("Products"),
                            Views.ul(items));
                }
            }""";

    public static final String SSR_HOST = """
            PageMeta meta = new PageMeta().lang("en");
            View page = Views.componentFactory(
                    () -> new ProductsPage(meta, catalogue.products()));

            Injector root = Injector.builder().name("ssr").build();
            try (ServerContext context = ServerContext.create(root, request)) {
                SsrResult result = SsrRenderer.renderPage(page, context, meta);
                return result.toResponse();
            } finally {
                root.close();
            }""";

    public static final String SSR_BOUNDARY = """
            final class SectionBoundary extends AbstractComponent
                    implements ServerErrorBoundary {

                @Override
                public View render() {
                    return Views.fragment(slot().toArray(View[]::new));
                }

                @Override
                public View fallback(Throwable error) {
                    return Views.p("Prices are temporarily unavailable.");
                }
            }""";

    public static final String SSR_RUN = """
            ./mvnw -pl examples/ssr -am -Prun package
            # open http://127.0.0.1:8080/ with JavaScript disabled""";

    public static final String SERVE = "python3 -m http.server 4280 -d examples/counter/target/webapp";

    public static final String COUNTER_COMPONENT = """
            package io.dejaview.examples.counter;

            import io.dejaview.core.AbstractComponent;
            import io.dejaview.core.View;
            import io.dejaview.core.Views;
            import io.dejaview.reactivity.Signal;

            final class CounterComponent extends AbstractComponent {

                private final Signal<Integer> count = new Signal<>(0);

                @Override
                public View render() {
                    int current = count.get();
                    return Views.div(
                                    Views.h1("DejaView"),
                                    Views.p("Count: " + current)
                                            .id("count")
                                            .attr("data-count", Integer.toString(current)),
                                    Views.button("Increment").onClick(this::increment),
                                    Views.p("Java. Reimagined for the Web.").classes("tagline"))
                            .classes("counter");
                }

                private void increment() {
                    count.update(value -> value + 1);
                }
            }""";

    public static final String SIGNALS_AND_COMPUTED = """
            private final Signal<Integer> count = new Signal<>(0);
            private final Computed<Integer> doubled = new Computed<>(() -> count.get() * 2);

            @Override
            public View render() {
                return Views.div(
                        Views.p("Count: " + count.get()),
                        Views.p("Double: " + doubled.get()),
                        Views.button("Increment").onClick(() -> count.update(v -> v + 1)));
            }""";

    public static final String CONDITIONAL = """
            private final Signal<Boolean> show = new Signal<>(true);

            @Override
            public View render() {
                return Views.div(
                        Views.h1("Conditional"),
                        Views.button("Toggle").onClick(() -> show.update(v -> !v)),
                        show.get()
                                ? Views.p("Visible")
                                : Views.fragment(Views.p("Hidden")));
            }""";

    public static final String KEYED_LIST = """
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
            }""";

    public static final String INPUTS = """
            // Parent: pushes an input into the child component.
            Views.componentFactory(UserCard.class, UserCard::new)
                    .input("username", name)
                    .on("select", selected -> lastEvent.set("Selected " + selected))
                    .slot("header", Views.strong(ada ? "Core team" : "Guest"))
                    .slot(Views.p(ada ? "Writes the renderer." : "Reviews every PR."))""";

    public static final String CHILD = """
            final class UserCard extends AbstractComponent {

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
            }""";

    public static final String LIFECYCLE = """
            @Override
            public void onInit() {
                // Invoked once per mounted node, before the first render.
            }

            @Override
            public void onUpdate() {
                // Invoked after each re-render of this component.
            }

            @Override
            public void onDestroy() {
                // Invoked when the component is removed from the tree.
            }""";

    public static final String DI = """
            public final class ApiClient {
                public String get(String path) {
                    return "response from " + path;
                }
            }

            Injector injector = Injector.builder()
                    .bind(ApiClient.class)
                    .to(injector -> new ApiClient())
                    .build();

            ApiClient api = injector.inject(ApiClient.class);""";

    public static final String ROUTER = """
            AtomicReference<Router> ref = new AtomicReference<>();
            Router router = Router.builder()
                    .routes(
                            Route.path("/users/:id")
                                    .component(() -> new UserScreen(ref.get()))
                                    .build(),
                            Route.path("/about")
                                    .component(() -> new AboutScreen())
                                    .build(),
                            Route.path("**")
                                    .component(() -> new HomeScreen(ref.get()))
                                    .build())
                    .history(RouterHistory.browser())
                    .build();
            ref.set(router);

            // In a component:
            public View render() {
                return Views.div(
                        Views.componentFactory(() -> new RouterOutlet(router)));
            }

            // Navigate from anywhere you hold the router:
            router.navigate("/users/42");""";

    public static final String ROUTE_PARAMS = """
            public View render() {
                // The outlet pushes :id from the URL as an input.
                String id = input("id", String.class);
                return Views.p("User id: " + id);
            }""";

    public static final String HTTP_PING = """
            private final Signal<String> result = new Signal<>("—");
            private final HttpClient client = HttpClient.builder().build();

            @Override
            public View render() {
                return Views.div(
                        Views.button("GET ping.json")
                                .id("http-ping")
                                .onClick(this::ping),
                        Views.p(result.get()).id("http-result"));
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
            }""";

    public static final String STYLING = """
            Views.button("Save").classes("btn", "btn-primary");
            Views.p("Tagline").classes("text-muted", "small");""";

    public static final String FORMS_CONTROL = """
            FormControl<String> email = new FormControl<>(
                    "",
                    Validators.required(),
                    Validators.email());

            FormGroup form = new FormGroup(Map.of(
                    "email", email,
                    "age", new FormControl<>(0, Validators.min(18))));

            ControlStatus status = form.status(); // VALID, INVALID, PENDING, DISABLED""";

    public static final String FORMS_VALIDATION = """
            private static void checkUsername(String value, ValidationCallback callback) {
                boolean taken = value.equals("admin");
                callback.complete(taken
                        ? ValidationErrors.of("taken")
                        : ValidationErrors.none());
            }

            FormControl<String> username =
                    new FormControl<>("", Validators.required(), Validators.minLength(3));
            username.addAsyncValidator(FormsExample::checkUsername);""";

    public static final String FORMS_GROUP = """
            FormGroup form = new FormGroup(Map.of(
                    "email", new FormControl<>("", Validators.required(), Validators.email()),
                    "tags", new FormArray()
                            .add(new FormControl<>("", Validators.required()))));

            form.errors().has("email.required");  // prefixed by child name
            form.errors().has("tags.0.required"); // prefixed by index""";

    public static final String FORMS_BINDING = """
            private final FormControl<String> email =
                    new FormControl<>("", Validators.email());
            private final FormControl<Boolean> accepted = new FormControl<>(false);
            private final FormGroup form =
                    new FormGroup(Map.of("email", email, "terms", accepted));

            @Override
            public View render() {
                return Views.element("form",
                        Views.element("label", Views.text("Email")).attr("for", "email"),
                        FormBindings.email("email", email),
                        FormBindings.checkbox("terms", accepted),
                        Views.button("Submit")
                                .attr("type", "button")
                                .onClick(this::submit));
            }

            private void submit() {
                FormSubmission<Map<String, Object>> result = form.submit();
                if (result.valid()) {
                    System.out.println(result.value());
                }
            }""";

    public static final String STATE_STORE = """
            private final StateStore<List<String>> cart =
                    new StateStore<>(List.of());

            @Override
            public View render() {
                // tracked read: changing the cart re-renders this component
                return Views.p("Items: " + cart.state().size());
            }

            cart.update(items -> {
                List<String> next = new ArrayList<>(items);
                next.add("book");
                return List.copyOf(next);
            });""";

    public static final String STATE_DERIVED = """
            Selector<List<String>, Integer> itemCount =
                    Selector.of("cart/itemCount", List::size);

            // lazy Computed owned by the store; disposed with it
            Computed<Integer> count = cart.select(itemCount);

            // cross-store projection without a global store of everything
            Computed<Integer> total =
                    Stores.combine(cart, discountPercent, (items, percent) ->
                            applyDiscount(subtotalOf(items), percent));""";

    public static final String STATE_ASYNC = """
            AsyncStore<String> orders = new AsyncStore<>((succeed, fail) ->
                    http.get("/api/orders", outcome -> {
                        if (outcome instanceof HttpResult.Success ok) {
                            succeed.accept(ok.body());
                        } else {
                            fail.accept(AsyncError.of("network-error", "orders request failed"));
                        }
                    }));

            AsyncState<String> state = orders.state(); // IDLE -> LOADING -> SUCCESS | FAILURE
            state.isLoading();
            state.data();   // last good value, kept while refreshing (stale-while-revalidate)
            state.error();  // Optional<AsyncError> with a stable, machine-readable code""";

    public static final String STATE_PERSIST = """
            PersistedStore<String> theme = new PersistedStore<>(
                    "dejaview-demo-theme",
                    new StateStore<>("dark"),
                    BrowserStorage.local(),
                    StateCodec.string());

            theme.restored();      // true when a stored value was found
            theme.store().state(); // restored value, or "dark" on first visit
            theme.store().set("light"); // written through to localStorage""";

    public static final String CLI_CREATE = """
            dejaview new my-app
            cd my-app
            dejaview run""";

    public static final String CLI_PROPERTIES = """
            # dejaview.properties — command-line flags take precedence
            port=4321
            host=127.0.0.1
            webappDir=target/webapp""";

    public static final String JAVA_MAIN = """
            public static void main(String[] args) {
                DejaView.mount(Views.componentFactory(CounterComponent::new), "app");
            }""";
}
