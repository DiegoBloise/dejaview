package io.dejaview.examples.showcase;

import io.dejaview.core.AbstractComponent;
import io.dejaview.core.View;
import io.dejaview.core.Views;
import io.dejaview.dom.DejaView;
import io.dejaview.reactivity.Signal;
import io.dejaview.http.HttpClient;
import io.dejaview.http.HttpResult;
import io.dejaview.router.Route;
import io.dejaview.router.Router;
import io.dejaview.router.RouterHistory;
import io.dejaview.router.RouterOutlet;
import io.dejaview.forms.AbstractControl;
import io.dejaview.forms.FormArray;
import io.dejaview.forms.FormBindings;
import io.dejaview.forms.FormControl;
import io.dejaview.forms.FormGroup;
import io.dejaview.forms.FormSubmission;
import io.dejaview.forms.ValidationCallback;
import io.dejaview.forms.ValidationErrors;
import io.dejaview.forms.Validators;
import org.teavm.jso.browser.Window;

/**
 * Entry point for the DejaView showcase and playground.
 *
 * <p>This class exposes one static method per example. The compiled TeaVM
 * bundle is loaded by the playground page inside an iframe with a query
 * parameter (e.g. {@code ?example=counter}). The {@link #main(String[])}
 * method reads the query parameter and dispatches to the corresponding
 * mount method.</p>
 */
public final class ShowcaseApp {

    private ShowcaseApp() {
    }

    public static void main(String[] args) {
        String example = getExampleFromQuery();
        switch (example) {
            case "greeting":
                mountGreeting("app");
                break;
            case "composition":
                mountComposition("app");
                break;
            case "conditional":
                mountConditional("app");
                break;
            case "usercard":
                mountUserCard("app");
                break;
            case "list":
                mountList("app");
                break;
            case "router":
                mountRouter("app");
                break;
            case "http":
                mountHttp("app");
                break;
            case "forms":
                mountForms("app");
                break;
            case "counter":
            default:
                mountCounter("app");
                break;
        }
    }

    /**
     * Reads the {@code example} query parameter from the browser URL.
     * Runs only in the browser (TeaVM target); on the JVM it returns
     * {@code "counter"} so unit tests can run.
     */
    private static String getExampleFromQuery() {
        try {
            org.teavm.jso.browser.Window window = org.teavm.jso.browser.Window.current();
            String search = window.getLocation().getSearch();
            if (search != null && search.startsWith("?")) {
                String params = search.substring(1);
                for (String pair : params.split("&")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && "example".equals(pair.substring(0, eq))) {
                        return pair.substring(eq + 1);
                    }
                }
            }
        } catch (Throwable ignored) {
            // On JVM or if JSO is not available, fall back to default.
        }
        return "counter";
    }

    /** Class-based counter example — the first implemented application. */
    public static void mountCounter(String hostId) {
        DejaView.mount(
                Views.componentFactory(CounterExample::new),
                hostId);
    }

    /** Functional component: the content supplier runs on every render. */
    public static void mountGreeting(String hostId) {
        Signal<String> name = new Signal<>("");
        DejaView.mount(Views.component(() ->
                Views.div(
                        Views.h1("Hello, " + name.get()),
                        Views.button("Set name")
                                .onClick(() -> name.set("DejaView")))
                        .classes("example")),
                hostId);
    }

    /** Nested components demonstrating composition and isolated state. */
    public static void mountComposition(String hostId) {
        DejaView.mount(
                Views.componentFactory(ParentComponent::new),
                hostId);
    }

    /** Conditional rendering through a fragment. */
    public static void mountConditional(String hostId) {
        DejaView.mount(
                Views.componentFactory(ConditionalComponent::new),
                hostId);
    }

    /** Inputs, outputs, slot projection and lifecycle. */
    public static void mountUserCard(String hostId) {
        DejaView.mount(
                Views.componentFactory(UserCardDemo::new),
                hostId);
    }

    /** Keyed list rendering with stable component identity. */
    public static void mountList(String hostId) {
        DejaView.mount(
                Views.componentFactory(KeyedListDemo::new),
                hostId);
    }

    /** Nested routing with browser history, parameters and back/forward. */
    public static void mountRouter(String hostId) {
        java.util.concurrent.atomic.AtomicReference<Router> ref = new java.util.concurrent.atomic.AtomicReference<>();
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

    /** Browser HTTP example: fetches ping.json and renders the body. */
    public static void mountHttp(String hostId) {
        DejaView.mount(Views.componentFactory(HttpPingDemo::new), hostId);
    }

    /** Reactive forms: validation, async checks, arrays and submission. */
    public static void mountForms(String hostId) {
        DejaView.mount(Views.componentFactory(FormsDemo::new), hostId);
    }

    /** Class-based counter, identical in shape to the examples/counter app. */
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
    }

    /** Parent renders two independent child components. */
    private static final class ParentComponent extends AbstractComponent {

        @Override
        public View render() {
            return Views.div(
                    Views.h1("Composition"),
                    Views.componentFactory(() -> new ChildComponent()),
                    Views.componentFactory(() -> new ChildComponent()));
        }
    }

    private static final class ChildComponent extends AbstractComponent {

        private final Signal<Integer> clicks = new Signal<>(0);

        @Override
        public View render() {
            return Views.button("Clicks: " + clicks.get())
                    .onClick(() -> clicks.update(v -> v + 1));
        }
    }

    /** Conditional rendering through a fragment. */
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
    }

    /** Parent pushing inputs, listening to outputs and projecting content. */
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
                    Views.p("Event: " + lastEvent.get()).id("event-log"))
                    .classes("example");
        }
    }

    /** Presentational child: reads inputs, projects slots, emits outputs. */
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
    }

    /** Keyed list reconciliation keeps DOM nodes stable across reorders. */
    private static final class KeyedListDemo extends AbstractComponent {

        private final Signal<java.util.List<String>> order =
                new Signal<>(java.util.List.of("Alpha", "Beta", "Gamma"));

        @Override
        public View render() {
            java.util.List<View> rows = new java.util.ArrayList<>();
            for (String label : order.get()) {
                rows.add(Views.li(label).key(label));
            }
            return Views.div(
                    Views.h1("Keyed list"),
                    Views.button("Rotate").onClick(this::rotate),
                    Views.ul(rows.toArray(View[]::new)));
        }

        private void rotate() {
            java.util.List<String> current = new java.util.ArrayList<>(order.get());
            current.add(0, current.remove(current.size() - 1));
            order.set(java.util.List.copyOf(current));
        }
    }

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
    }

    /** Shell for the router example: mounts the outlet once. */
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

    /** Fallback screen (catch-all route). Linked from all other screens. */
    private static final class RouterHomeScreen extends AbstractComponent {

        private final Router router;

        RouterHomeScreen(Router router) {
            this.router = router;
        }

        @Override
        public View render() {
            return Views.div(
                    Views.h2("Home").id("router-home"),
                    Views.button("Open user 42")
                            .id("router-open-user")
                            .onClick(() -> router.navigate("/users/42")),
                    Views.button("About")
                            .id("router-open-about")
                            .onClick(() -> router.navigate("/about")));
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
    }

    /** Static screen to demonstrate navigation between named routes. */
    private static final class RouterAboutScreen extends AbstractComponent {

        private final Router router;

        RouterAboutScreen(Router router) {
            this.router = router;
        }

        @Override
        public View render() {
            return Views.div(
                    Views.h2("About").id("router-about"),
                    Views.button("Home")
                            .id("router-about-home")
                            .onClick(() -> router.navigate("/")));
        }
    }

    /**
     * Reactive forms demo: synchronous and asynchronous validation, aggregated
     * state, a dynamic array and a submission snapshot.
     */
    private static final class FormsDemo extends AbstractComponent {

        private static final String FIELD_STYLE =
                "display:grid;gap:0.25rem;margin:0";
        private static final String INPUT_STYLE =
                "padding:0.45rem 0.55rem;border-radius:6px;border:1px solid #3b4354;"
                        + "background:#1b1f2a;color:#f5f7fb;font:inherit";

        private final FormControl<String> name =
                new FormControl<>("", Validators.required(), Validators.minLength(2));
        private final FormControl<String> email =
                new FormControl<>("", Validators.required(), Validators.email());
        private final FormControl<String> username =
                new FormControl<>("", Validators.required(), Validators.minLength(3));
        private final FormControl<Boolean> newsletter = new FormControl<>(false);
        private final FormControl<String> plan = new FormControl<>("free");
        private final FormArray tags = new FormArray()
                .add(new FormControl<>("", Validators.required()));
        private final FormGroup form = new FormGroup();
        private final Signal<String> message = new Signal<>("No submission yet.");

        FormsDemo() {
            username.addAsyncValidator(FormsDemo::checkUsername);
            form.addControl("name", name)
                    .addControl("email", email)
                    .addControl("username", username)
                    .addControl("newsletter", newsletter)
                    .addControl("plan", plan)
                    .addControl("tags", tags);
        }

        @Override
        public View render() {
            return Views.div(
                            Views.h1("Reactive forms"),
                            Views.p("Status: " + form.status())
                                    .id("forms-status")
                                    .classes("forms-status")
                                    .attr("role", "status")
                                    .attr("aria-live", "polite"),
                            field("Name", "forms-name", name,
                                    FormBindings.text("forms-name", name).style(INPUT_STYLE)),
                            field("Email", "forms-email", email,
                                    FormBindings.email("forms-email", email).style(INPUT_STYLE)),
                            field("Username", "forms-username", username,
                                    FormBindings.text("forms-username", username).style(INPUT_STYLE)),
                            Views.div(tagRows())
                                    .classes("forms-tags")
                                    .style("display:grid;gap:0.5rem;margin:0"),
                            Views.button("Add tag")
                                    .id("forms-add-tag")
                                    .attr("type", "button")
                                    .onClick(this::addTag),
                            Views.div(
                                    Views.element("label", Views.text("Plan"))
                                            .attr("for", "forms-plan"),
                                    FormBindings.select("forms-plan", plan, java.util.List.of(
                                            new FormBindings.SelectOption("free", "Free"),
                                            new FormBindings.SelectOption("pro", "Pro"),
                                            new FormBindings.SelectOption("team", "Team")))
                                            .style(INPUT_STYLE),
                                    Views.element("label", Views.text("Newsletter"))
                                            .attr("for", "forms-newsletter"),
                                    FormBindings.checkbox("forms-newsletter", newsletter))
                                    .style(FIELD_STYLE),
                            Views.button("Submit")
                                    .id("forms-submit")
                                    .attr("type", "button")
                                    .onClick(this::submit),
                            Views.p(message.get())
                                    .id("forms-result")
                                    .classes("forms-result")
                                    .attr("role", "status")
                                    .attr("aria-live", "polite"))
                    .classes("example", "forms-example")
                    .style("display:grid;gap:0.85rem;max-width:28rem;align-items:start");
        }

        private View field(String label, String id, AbstractControl<?> control, View input) {
            return Views.div(
                            Views.element("label", Views.text(label)).attr("for", id),
                            input,
                            errorFor(control, id + "-error"),
                            pendingFor(control, id + "-pending"))
                    .style(FIELD_STYLE);
        }

        private View tagRows() {
            int count = tags.size();
            View[] rows = new View[count];
            for (int index = 0; index < count; index++) {
                AbstractControl<?> tag = tags.at(index);
                String id = "forms-tag-" + index;
                int row = index;
                rows[index] = Views.div(
                                Views.div(
                                                Views.element("label", Views.text("Tag " + (index + 1)))
                                                        .attr("for", id),
                                                FormBindings.text(id, tagControl(tag)).style(INPUT_STYLE),
                                                errorFor(tag, id + "-error"))
                                        .style(FIELD_STYLE),
                                Views.button("Remove")
                                        .id("forms-remove-tag-" + index)
                                        .attr("type", "button")
                                        .onClick(() -> tags.removeAt(row)))
                        .style("display:flex;gap:0.5rem;align-items:end");
            }
            return Views.fragment(rows);
        }

        private void addTag() {
            tags.add(new FormControl<>("", Validators.required()));
        }

        private void submit() {
            FormSubmission<java.util.Map<String, Object>> submission = form.submit();
            if (submission.valid()) {
                message.set("Submitted: " + submission.value());
            } else {
                message.set("Please fix the highlighted fields.");
            }
        }

        private static View errorFor(AbstractControl<?> control, String id) {
            String text = control.touched() ? messageFor(control.errors()) : "";
            return Views.p(text)
                    .id(id)
                    .classes("form-error")
                    .style("color:#f7768e;margin:0;font-size:0.85rem")
                    .attr("role", "alert");
        }

        private static View pendingFor(AbstractControl<?> control, String id) {
            return Views.p(control.pending() ? "Checking availability…" : "")
                    .id(id)
                    .classes("form-hint")
                    .style("color:#9aa5b8;margin:0;font-size:0.8rem");
        }

        private static String messageFor(ValidationErrors errors) {
            if (errors.has("required")) {
                return "This field is required.";
            }
            if (errors.has("email")) {
                return "Enter a valid email address.";
            }
            if (errors.has("minLength")) {
                return "Use at least 3 characters.";
            }
            if (errors.has("taken")) {
                return "That username is already taken.";
            }
            return "";
        }

        @SuppressWarnings("unchecked")
        private static FormControl<String> tagControl(AbstractControl<?> control) {
            return (FormControl<String>) control;
        }

        private static void checkUsername(String value, ValidationCallback callback) {
            try {
                Window.setTimeout(
                        () -> callback.complete(isTaken(value)
                                ? ValidationErrors.of("taken")
                                : ValidationErrors.none()),
                        600);
            } catch (Throwable unavailable) {
                callback.complete(isTaken(value)
                        ? ValidationErrors.of("taken")
                        : ValidationErrors.none());
            }
        }

        private static boolean isTaken(String value) {
            return value.equals("admin") || value.equals("dejaview");
        }
    }
}
