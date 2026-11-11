# DejaView — Architecture & Technical Specification

> **Com certeza você já viu isso antes.**
>
> A Java-first frontend framework.

**Status:** Architecture Proposal (long-term target)  
**Version:** 0.1.0  
**Language:** Java  
**Compiler:** TeaVM  
**Compilation Target:** JavaScript  
**Rendering:** CSR, SSR, SSG  
**License:** TBD

> **Implementation note (2026-11-11).** This document describes the long-term
> target architecture. The current repository implements only the foundation
> described in [ADR-0003](ADR/0003-initial-module-boundaries.md) and
> [ADR-0007](ADR/0007-first-milestone-scope.md): `dejaview-core` (immutable view
> model), `dejaview-dom` (DOM contracts, first-mount renderer, TeaVM JSO adapter)
> and `examples/counter`. TeaVM 0.16.0 with Java 25 has been validated; see
> [`COMPILER.md`](COMPILER.md). The authoritative implemented-versus-planned
> list is [`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md). Modules and
> capabilities described below but absent from that list are proposals, not
> implemented features.

---

# 1. Visão geral

## 1.1. Introdução

DejaView é um framework frontend Java-first projetado para permitir o desenvolvimento de aplicações web modernas, escaláveis, reativas e componentizadas utilizando Java como linguagem principal.

O framework busca reunir conceitos e funcionalidades presentes em Angular, React e Vue.js, oferecendo uma experiência de desenvolvimento familiar aos desenvolvedores frontend, aproveitando o ecossistema, a tipagem estática e os recursos da plataforma Java.

A proposta não é simplesmente transformar Java em HTML, mas desenvolver um ecossistema completo de frontend, contendo:

- Runtime próprio.
- Sistema de componentes.
- Engine de renderização.
- Sistema de reatividade.
- Gerenciamento de estado.
- Compilador e pipeline de build.
- Roteamento.
- Dependency Injection.
- Formulários reativos.
- HTTP Client.
- SSR, CSR e SSG.
- Hydration.
- CLI.
- DevTools.
- Sistema de plugins e extensões.

## 1.2. Objetivos

1. Permitir desenvolvimento frontend utilizando Java.
2. Oferecer uma API intuitiva, consistente e fortemente tipada.
3. Criar um runtime eficiente para aplicações web.
4. Implementar um sistema reativo baseado em Signals.
5. Suportar diferentes estratégias de renderização.
6. Compilar aplicações Java para JavaScript.
7. Integrar-se ao ecossistema Java existente.
8. Facilitar a criação de aplicações pequenas e enterprise.
9. Oferecer ferramentas profissionais de desenvolvimento.
10. Manter uma arquitetura modular e extensível.

## 1.3. Filosofia

- Type safety.
- Performance.
- Developer Experience (DX).
- Modularidade.
- Extensibilidade.
- Acessibilidade.
- Compatibilidade com padrões web.
- Separação de responsabilidades.
- Baixo acoplamento.
- Otimização em build time.
- Simplicidade na API pública.

# 1.4. Baseline de Java e Build

O projeto adotará **Java 25 LTS como versão mínima de desenvolvimento** e **Apache Maven como sistema oficial de build e gerenciamento de dependências**.

Diretrizes:

- JDK 25 será necessário para compilar e desenvolver o framework.
- O Maven Wrapper será utilizado para padronizar a versão do Maven entre ambientes.
- O POM raiz centralizará propriedades, versões de dependências e configurações de plugins.
- `maven.compiler.release` deverá ser configurado como `25`.
- Os módulos deverão declarar apenas suas dependências diretas.
- A compatibilidade de cada recurso da linguagem Java 25 com o TeaVM deverá ser validada antes de seu uso no código que será compilado para JavaScript.
- A adoção de Java 25 como baseline não significa que toda a API do JDK esteja disponível no navegador.

Exemplo de configuração inicial do POM raiz:

```xml
<properties>
    <maven.compiler.release>25</maven.compiler.release>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

Comandos padronizados:

```bash
# Compilar todos os módulos
./mvnw clean compile

# Executar testes
./mvnw test

# Empacotar os módulos
./mvnw clean package

# Instalar artefatos no repositório Maven local
./mvnw clean install
```

# 2. Visão arquitetural

## 2.1. Arquitetura geral

```text
                 DEJAVIEW ECOSYSTEM
                         |
          +--------------+--------------+
          |                             |
     APPLICATION                    TOOLING
          |                             |
   +------+-------+              +------+------+
   |              |              |             |
COMPONENTS      ROUTER           CLI         DEVTOOLS
   |              |              |             |
   +------+-------+              +-------------+
          |
     DEJAVIEW CORE
          |
  +-------+---------+
  |       |         |
REACTIVITY RENDERER COMPONENT MODEL
  |       |         |
  +-------+---------+
          |
     PLATFORM LAYER
          |
   +------+------+------+
   |             |      |
  DOM          SERVER  STATIC
   |             |      |
  CSR           SSR    SSG
   |             |      |
   +------+------+------+
          |
     COMPILER LAYER
          |
        TEAVM
          |
      JAVASCRIPT
```

A arquitetura deverá permitir substituir implementações internas sem alterar desnecessariamente a API pública.

## 2.2. Separação entre Core e Platform

O `dejaview-core` não deverá depender diretamente das APIs do navegador.

O Core será responsável por:

- Componentes.
- Estado.
- Reatividade.
- Lifecycle.
- Context.
- Computed values.
- Effects.
- Render tree.
- Agendamento de atualizações.

As implementações específicas de plataforma serão responsáveis pela comunicação com DOM, servidor e geração de HTML estático.

# 3. Compiler e JavaScript Target

## 3.1. Java como linguagem principal

O desenvolvedor escreverá componentes, serviços, estados e lógica de aplicação em Java.

Exemplo:

```java
@Component
public class CounterComponent {

    @State
    private int count = 0;

    public View render() {
        return div(
            h1("DejaView Counter"),
            p("Count: " + count),
            button("Increment")
                .onClick(() -> count++)
        );
    }
}
```

A API deverá ser fortemente tipada e utilizar recursos modernos da linguagem Java 25, respeitando as limitações de compilação e execução impostas pelo TeaVM.

## 3.2. Compiler

O DejaView utilizará TeaVM como tecnologia inicial de compilação. A baseline da linguagem e do bytecode do projeto será Java 25 LTS. Antes de habilitar recursos específicos dessa versão, será necessário validar o suporte efetivo da versão do TeaVM adotada.

Responsabilidades:

- Compilar bytecode Java.
- Gerar artefatos JavaScript.
- Resolver dependências.
- Executar processamento de anotações.
- Realizar otimizações.
- Gerar source maps quando suportado.
- Identificar incompatibilidades com o ambiente browser.
- Gerar metadados para o runtime.

O compilador deverá abstrair os detalhes do TeaVM para permitir substituição futura da tecnologia. A versão adotada deverá ser validada quanto ao suporte a Java 25, incluindo bytecode, recursos da linguagem, bibliotecas utilizadas e geração de JavaScript. Não se deve presumir que todo recurso do JDK 25 seja compatível com o ambiente browser.

**Estado validado:** TeaVM **0.16.0** compila bytecode Java 25 (class file major
version 69) e gera JavaScript executável em Chromium. A versão 0.12.0 foi
rejeitada por não suportar esse bytecode. A matriz de compatibilidade, as
restrições de classlib e a configuração Maven validada estão em
[`COMPILER.md`](COMPILER.md). A abstração de compilador ainda **não** foi
implementada; o TeaVM está configurado diretamente no POM do exemplo.

## 3.3. JavaScript Runtime

JavaScript será o único target de compilação para execução no navegador.

Características:

- Integração direta com DOM.
- Compatibilidade com APIs Web.
- Comunicação com bibliotecas JavaScript.
- Suporte a CSR.
- Suporte à hydration.
- Integração com DevTools.
- Acesso a Fetch API, History API e Web Storage.
- Compatibilidade com bibliotecas e ferramentas do ecossistema JavaScript.

O framework deverá priorizar uma integração eficiente com o navegador, evitando abstrações desnecessárias sobre as APIs nativas da Web.

## 3.4. Build

```bash
# Compilar aplicação
dejaview build

# Build de desenvolvimento
dejaview build --development

# Build de produção
dejaview build --production

# Analisar artefatos
dejaview analyze
```

O processo de build deverá suportar otimizações, geração de bundles, resolução de dependências e preparação dos recursos necessários para execução no navegador.

> Os comandos `dejaview build` acima descrevem a CLI planejada (Phase 5). No
> estado atual, o build é executado com Maven: `./mvnw clean verify` gera
> `examples/counter/target/webapp/`.

# 4. Rendering Engine

O DejaView deverá oferecer diferentes estratégias de renderização.

## 4.1. Client-Side Rendering (CSR)

Renderização executada no navegador.

Fluxo:

1. Browser carrega o HTML inicial.
2. Browser carrega os arquivos JavaScript.
3. Runtime DejaView é inicializado.
4. Componentes são instanciados.
5. Render tree é construída.
6. DOM é atualizado.
7. Eventos são registrados.
8. Aplicação passa a funcionar.

Características:

- Renderização dinâmica.
- Atualizações reativas.
- Navegação client-side.
- Gerenciamento de estado.
- Event handling.
- Lazy loading.

CSR será a primeira estratégia implementada.

## 4.2. Server-Side Rendering (SSR)

Renderização dos componentes no servidor.

O servidor executará a lógica Java e produzirá HTML antes de enviar a resposta ao navegador.

Fluxo:

```text
HTTP REQUEST
     |
JAVA SERVER
     |
DEJAVIEW SERVER RUNTIME
     |
COMPONENT RENDERING
     |
HTML GENERATION
     |
HTTP RESPONSE
     |
BROWSER
     |
HYDRATION
```

Características:

- HTML pré-renderizado.
- SEO.
- Melhor disponibilidade inicial do conteúdo.
- Integração com aplicações Java.
- Possibilidade de streaming de HTML.
- Suporte a metadata dinâmica.

O SSR deverá utilizar o mesmo modelo de componentes do CSR, respeitando as limitações do ambiente server-side.

## 4.3. Hydration

A hydration permitirá transformar uma interface SSR em uma aplicação interativa sem reconstruir desnecessariamente toda a árvore DOM.

Responsabilidades:

- Identificar componentes existentes.
- Associar eventos.
- Restaurar estado serializado.
- Reconstruir dependências reativas.
- Associar elementos aos respectivos componentes.
- Evitar divergências entre HTML do servidor e renderização cliente.

O framework deverá implementar mecanismos para detectar hydration mismatches.

Estratégias futuras:

- Full hydration.
- Selective hydration.
- Lazy hydration.
- Partial hydration.
- Islands architecture.

## 4.4. Static Site Generation (SSG)

O SSG permitirá gerar páginas HTML durante o processo de build.

Características:

- Geração de HTML estático.
- Pré-renderização de rotas.
- Metadata estática e dinâmica.
- Assets otimizados.
- Integração com hospedagens estáticas.
- Possibilidade de hydration no cliente.

Exemplo:

```bash
dejaview generate --static
```

## 4.5. Incremental Static Regeneration (ISR)

Funcionalidade planejada para versões futuras.

Permitir regeneração de páginas estáticas após determinado intervalo ou evento.

Possibilidades:

- Revalidação temporal.
- Revalidação sob demanda.
- Cache de páginas.
- Regeneração de rotas específicas.

## 4.6. Rendering Strategy por rota

O desenvolvedor poderá configurar a estratégia individualmente.

Exemplo conceitual:

```java
@Route(path = "/dashboard", rendering = Rendering.CSR)
public class DashboardPage {
}

@Route(path = "/products", rendering = Rendering.SSR)
public class ProductsPage {
}

@Route(path = "/blog", rendering = Rendering.SSG)
public class BlogPage {
}
```

# 5. Component System

## 5.1. Componentes

O sistema de componentes deverá oferecer estruturas independentes, reutilizáveis e com responsabilidades específicas.

```java
@Component
public class UserCard {

    @Input
    private String username;

    public View render() {
        return div(
            h2(username)
        );
    }
}
```

## 5.2. Component Composition

Suportar:

- Componentes aninhados.
- Componentes reutilizáveis.
- Componentes dinâmicos.
- Slots.
- Templates.
- Componentes genéricos.
- Componentes condicionais.
- Componentes iterativos.

## 5.3. Inputs e Outputs

Comunicação entre componentes.

```java
@Input
private User user;

@Output
private EventEmitter<User> onSelect;
```

Os inputs deverão suportar validação e valores padrão.

## 5.4. Lifecycle Hooks

Implementar:

- `onInit()`
- `onMount()`
- `onUpdate()`
- `onDestroy()`
- `onError()`

Os hooks deverão possuir semântica documentada e consistente entre os ambientes de execução.

## 5.5. Component Templates

O framework deverá oferecer uma API declarativa para criação de interfaces.

```java
return div(
    h1("Welcome"),
    when(isAuthenticated,
        dashboard()
    ),
    each(users, user ->
        userCard(user)
    )
);
```

A API deverá ser tipada e permitir composição de elementos sem depender de strings HTML arbitrárias.

# 6. Reactivity System

Um dos principais diferenciais técnicos do DejaView será o sistema de reatividade.

## 6.1. Signals

Implementar um sistema de Signals inspirado em Angular Signals, SolidJS e outros modelos de reatividade granular.

```java
Signal<Integer> count = signal(0);

count.set(10);

count.update(value -> value + 1);
```

## 6.2. Computed

Valores derivados automaticamente.

```java
Signal<Integer> count = signal(10);

Computed<Integer> doubled =
    computed(() -> count.get() * 2);
```

## 6.3. Effects

Executar efeitos quando dependências reativas forem modificadas.

```java
effect(() -> {
    System.out.println(count.get());
});
```

## 6.4. Requisitos da engine reativa

- Dependency tracking.
- Dependency graph.
- Fine-grained updates.
- Automatic dependency registration.
- Batching.
- Scheduling.
- Computed values.
- Effect cleanup.
- Circular dependency detection.
- Memory leak prevention.
- Error propagation.

## 6.5. Change Detection

O framework deverá evitar atualizações desnecessárias.

A estratégia preferencial será reatividade granular, atualizando somente as partes afetadas da interface.

Não deverá depender obrigatoriamente de uma verificação completa de todos os componentes após cada evento.

# 7. State Management

Oferecer gerenciamento de estado em diferentes níveis.

## 7.1. Local State

Estado pertencente a um componente.

## 7.2. Shared State

Estado compartilhado entre componentes.

## 7.3. Global State

Stores globais para aplicações maiores.

## 7.4. Derived State

Valores derivados de outros estados.

## 7.5. Async State

Gerenciamento de operações assíncronas.

Estados:

- Idle.
- Loading.
- Success.
- Error.

## 7.6. Persisted State

Possibilidade de persistência utilizando:

- LocalStorage.
- SessionStorage.
- IndexedDB.

A persistência deverá ser configurável e independente da implementação central de Signals.

# 8. Dependency Injection

Implementar um container próprio de Dependency Injection.

Características:

- Singleton.
- Transient.
- Scoped.
- Constructor injection.
- Provider registration.
- Qualifiers.
- Optional dependencies.
- Circular dependency detection.
- Lifecycle management.

Exemplo:

```java
@Service
@Singleton
public class UserService {
}

@Component
public class UserComponent {

    private final UserService userService;

    @Inject
    public UserComponent(UserService userService) {
        this.userService = userService;
    }
}
```

O sistema deverá suportar providers registrados manualmente e por anotações.

Também deverá ser possível integrar serviços externos.

# 9. Router

Implementar um sistema completo de roteamento.

## 9.1. Recursos

- Client-side navigation.
- Nested routes.
- Dynamic routes.
- Route parameters.
- Query parameters.
- Route guards.
- Lazy loading.
- Route resolvers.
- Navigation events.
- Redirects.
- 404 handling.
- Route metadata.
- Route-level rendering strategies.

Exemplo:

```java
@Route(path = "/users/:id")
public class UserPage {
}
```

## 9.2. Navigation

Suportar History API e navegação baseada em URL.

## 9.3. Guards

Permitir validações antes de acessar determinadas rotas.

Exemplos:

- Authentication Guard.
- Authorization Guard.
- Unsaved Changes Guard.

## 9.4. Lazy Loading

Carregar funcionalidades e rotas sob demanda.

# 10. HTTP Client

Criar uma API HTTP própria.

Recursos:

- GET.
- POST.
- PUT.
- PATCH.
- DELETE.
- Headers.
- Query parameters.
- Interceptors.
- Request cancellation.
- Timeout.
- Error handling.
- JSON serialization.
- Multipart requests.
- Authentication headers.

Exemplo:

```java
User user = http
    .get("/api/users/1")
    .retrieve(User.class);
```

A implementação deverá abstrair as diferenças entre browser e server.

No navegador, poderá utilizar Fetch API.

No servidor, poderá utilizar uma implementação compatível com o ambiente Java.

# 11. Forms

Sistema completo de formulários.

## 11.1. Form Controls

- FormControl.
- FormGroup.
- FormArray.
- FormBuilder.

## 11.2. Validation

- Required.
- Min.
- Max.
- MinLength.
- MaxLength.
- Pattern.
- Custom validators.
- Async validators.

## 11.3. Form State

- Valid.
- Invalid.
- Dirty.
- Pristine.
- Touched.
- Untouched.
- Pending.

## 11.4. Form Features

- Reactive Forms.
- Typed Forms.
- Dynamic Forms.
- Nested Forms.
- Custom Form Components.
- Async Validation.

# 12. Styling System

O framework deverá oferecer liberdade na utilização de CSS.

Suportar:

- CSS.
- CSS Modules.
- Scoped Styles.
- CSS Variables.
- Design Tokens.
- Theme System.
- Dark Mode.
- Responsive Design.

Possibilitar integração com Tailwind CSS, Bootstrap e bibliotecas externas.

O framework não deverá impor uma solução de estilização específica.

# 13. Performance e Optimization

A performance deverá ser considerada desde a arquitetura inicial.

## 13.1. Runtime Optimization

- Fine-grained reactivity.
- Efficient DOM updates.
- Component memoization.
- Update batching.
- Event delegation quando apropriado.
- Efficient scheduling.
- Reduced allocations.
- Resource cleanup.

## 13.2. Build Optimization

- Dead Code Elimination.
- Tree Shaking.
- Minification.
- Code Splitting.
- Lazy Loading.
- Dependency optimization.
- Production builds.
- Asset optimization.

## 13.3. Memory Management

- Lifecycle cleanup.
- Event listener cleanup.
- Effect disposal.
- Subscription management.
- Circular reference prevention.
- Memory profiling.

## 13.4. Performance Benchmarks

Criar benchmarks comparando o DejaView com React, Angular e Vue.js.

Cenários:

- Initial rendering.
- Component creation.
- List rendering.
- State updates.
- Reactive dependency propagation.
- Memory consumption.
- Bundle size.
- Hydration.
- Navigation.

Os resultados deverão ser reproduzíveis e utilizar versões e condições de teste documentadas.

# 14. Error Handling

Implementar sistema centralizado de tratamento de erros.

Recursos:

- Component error boundaries.
- Global error handler.
- Lifecycle error handling.
- Async error handling.
- Rendering errors.
- HTTP errors.
- Hydration errors.
- Development error overlays.

Em desenvolvimento, fornecer mensagens detalhadas e rastreáveis.

Em produção, evitar exposição de informações internas e sensíveis.

# 15. Security

O DejaView deverá considerar segurança desde o início.

Requisitos:

- Escape automático de conteúdo textual.
- Proteção contra XSS.
- Sanitização de HTML quando necessário.
- Política de CSP.
- Proteção de dados sensíveis.
- Segurança na serialização de estado SSR.
- Prevenção de exposição de variáveis server-only.
- Separação entre código server-side e client-side.
- Tratamento seguro de URLs.

Nunca executar conteúdo HTML arbitrário sem uma API explícita e documentada.

# 16. Accessibility

Acessibilidade será parte integrante do framework.

Suportar:

- ARIA attributes.
- Semantic HTML.
- Keyboard navigation.
- Focus management.
- Screen reader compatibility.
- Accessible forms.
- Accessible routing.
- Focus restoration.

O framework deverá facilitar a criação de interfaces acessíveis sem esconder os recursos nativos do HTML.

# 17. Internationalization (i18n)

Implementar suporte a internacionalização.

Recursos:

- Translation resources.
- Dynamic locale switching.
- Date formatting.
- Number formatting.
- Currency formatting.
- Pluralization.
- Locale detection.
- Lazy-loaded translations.

A implementação deverá aproveitar as APIs de internacionalização disponíveis no ambiente de execução.

# 18. CLI

Criar uma CLI própria para gerenciamento de projetos.

## 18.1. Commands

```bash
# Criar projeto
dejaview create my-app

# Iniciar desenvolvimento
dejaview dev

# Compilar
dejaview build

# Executar testes
dejaview test

# Gerar componente
dejaview generate component user-card

# Gerar serviço
dejaview generate service user

# Gerar página
dejaview generate page dashboard

# Gerar projeto estático
dejaview generate --static

# Analisar dependências
dejaview analyze
```

## 18.2. Development Server

Implementar:

- Hot Reload.
- Live Reload.
- Error Overlay.
- Build monitoring.
- Incremental compilation.
- Development proxy.
- Source maps.

# 19. DevTools

Desenvolver ferramentas próprias de debugging.

Funcionalidades:

- Component Inspector.
- Component Tree.
- Signal Inspector.
- Dependency Graph.
- State Inspector.
- Event Inspector.
- Rendering Profiler.
- Performance Timeline.
- Hydration Inspector.
- Router Inspector.

O DevTools deverá possuir integração com o runtime e funcionar preferencialmente por meio de uma extensão de navegador.

# 20. Testing

Implementar uma infraestrutura de testes.

## 20.1. Unit Testing

Testar:

- Signals.
- Computed.
- Effects.
- Components.
- Services.
- Dependency Injection.
- Router.
- HTTP Client.

## 20.2. Component Testing

Permitir renderizar componentes isoladamente e verificar seus comportamentos.

## 20.3. Integration Testing

Testar comunicação entre componentes, serviços e módulos.

## 20.4. End-to-End Testing

Possibilitar integração com ferramentas como Playwright.

## 20.5. SSR Testing

Validar:

- HTML gerado.
- Metadata.
- Estado serializado.
- Hydration.
- Divergências de renderização.

# 21. Ecosystem e Extensibility

O DejaView deverá permitir extensões e bibliotecas externas.

## 21.1. Plugin System

Plugins poderão adicionar:

- Build integrations.
- Compiler extensions.
- CLI commands.
- Development tools.
- Rendering adapters.
- Custom transforms.

## 21.2. UI Libraries

Possibilitar bibliotecas de componentes reutilizáveis.

Exemplos futuros:

- DejaView UI.
- DejaView Material.
- DejaView Charts.
- DejaView Icons.

## 21.3. Third-party Integration

Facilitar integração com:

- Spring Boot.
- Spring Security.
- Jakarta Validation.
- Jackson.
- Hibernate.
- PostgreSQL.
- WebSockets.
- GraphQL.

# 22. Spring Boot Integration

Criar integração oficial com Spring Boot.

Objetivos:

- SSR utilizando aplicações Spring Boot.
- Controllers.
- Server-side Dependency Injection.
- Compartilhamento de DTOs.
- Integração com autenticação.
- HTTP endpoints.
- Server-side data fetching.
- SSR middleware.

O DejaView não deverá depender obrigatoriamente do Spring Boot.

A integração deverá ser disponibilizada como módulo separado.

# 23. Project Structure

## 23.1. Estrutura implementada

Estrutura atual do repositório, limitada aos módulos justificados pelo primeiro
milestone:

```text
dejaview/
│
├── docs/
│   ├── ARCHITECTURE.md
│   ├── COMPILER.md
│   ├── IMPLEMENTATION_STATUS.md
│   └── ADR/
│
├── dejaview-core/
├── dejaview-dom/
├── examples/
│   └── counter/
├── .mvn/
│
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

Direção de dependências: `examples/counter` → `dejaview-dom` →
`dejaview-core`. O `dejaview-core` não importa APIs de navegador nem TeaVM.

## 23.2. Estrutura alvo (proposta)

Estrutura de longo prazo proposta para o monorepo. Módulos ainda não criados são
planejamento, não funcionalidade disponível:

```text
dejaview/
│
├── dejaview-core/
├── dejaview-reactivity/
├── dejaview-renderer/
├── dejaview-dom/
├── dejaview-server/
├── dejaview-ssr/
├── dejaview-ssg/
├── dejaview-hydration/
├── dejaview-compiler/
├── dejaview-router/
├── dejaview-http/
├── dejaview-forms/
├── dejaview-di/
├── dejaview-i18n/
├── dejaview-spring-boot/
├── dejaview-cli/
├── dejaview-devtools/
├── dejaview-testing/
│
├── examples/
├── benchmarks/
└── docs/
```

A estrutura poderá evoluir conforme as responsabilidades dos módulos forem consolidadas. O repositório utiliza Apache Maven em arquitetura multi-module, com um `pom.xml` raiz, um `pom.xml` por módulo e Maven Wrapper (`mvnw`/`mvnw.cmd`). Módulos vazios não são criados antes de existir uma responsabilidade concreta.

# 24. Browser Support

O framework deverá definir uma matriz de compatibilidade.

Avaliar:

- Chromium.
- Firefox.
- Safari.
- Mobile browsers.
- JavaScript runtime compatibility.
- Compatibilidade com APIs Web utilizadas.

O target JavaScript deverá possuir a maior cobertura possível, com documentação clara dos requisitos mínimos de navegador.

# 25. Development Roadmap

O desenvolvimento deverá ocorrer incrementalmente.

## Phase 1 — Foundation

- [x] Definir arquitetura do Core.
- [x] Configurar monorepo Maven multi-module.
- [x] Integrar TeaVM 0.16.0.
- [x] Implementar compilação Java → JavaScript.
- [x] Criar primeiro runtime mínimo (`DejaView.mount` + `DomRenderer`).
- [x] Implementar DOM adapter.
- [x] Renderizar primeira árvore de views (primeiro mount; sistema de componentes é Phase 3).
- [x] Implementar event handling básico (listeners de click).

**Objetivo:** executar uma aplicação Java no navegador utilizando DejaView.

**Estado:** o primeiro milestone foi implementado e é validado por testes JVM e
por um teste E2E em Chromium. O escopo exato — primeiro mount apenas, sem
diffing, reatividade ou hidratação — está em
[ADR-0007](ADR/0007-first-milestone-scope.md) e
[`IMPLEMENTATION_STATUS.md`](IMPLEMENTATION_STATUS.md).

## Phase 2 — Reactivity Engine

- [ ] Implementar Signal.
- [ ] Implementar Computed.
- [ ] Implementar Effect.
- [ ] Dependency tracking.
- [ ] Dependency graph.
- [ ] Batching.
- [ ] Scheduling.
- [ ] Cleanup.
- [ ] Testes de reatividade.

**Objetivo:** atualizar interfaces automaticamente sem renderizações desnecessárias.

## Phase 3 — Component System

- [ ] Component annotations.
- [ ] Component lifecycle.
- [ ] Inputs e Outputs.
- [ ] Component composition.
- [ ] Slots.
- [ ] Conditional rendering.
- [ ] List rendering.
- [ ] Context API.

**Objetivo:** construir aplicações utilizando componentes independentes.

## Phase 4 — Application Framework

- [ ] Dependency Injection.
- [ ] Router.
- [ ] Route Guards.
- [ ] Lazy Loading.
- [ ] HTTP Client.
- [ ] Reactive Forms.
- [ ] Error Boundaries.
- [ ] Styling integrations.

**Objetivo:** desenvolver aplicações completas.

## Phase 5 — Compiler e Tooling

- [ ] Compiler abstraction.
- [ ] Annotation processing.
- [ ] Production build.
- [ ] Tree Shaking.
- [ ] Code Splitting.
- [ ] Source Maps.
- [ ] CLI.
- [ ] Development Server.
- [ ] Hot Reload.

**Objetivo:** proporcionar uma experiência profissional de desenvolvimento.

## Phase 6 — SSR e SSG

- [ ] Server renderer.
- [ ] HTML serialization.
- [ ] SSR integration.
- [ ] State serialization.
- [ ] Hydration.
- [ ] Hydration mismatch detection.
- [ ] Static Site Generation.
- [ ] Route pre-rendering.

**Objetivo:** oferecer diferentes estratégias de renderização.

## Phase 7 — Advanced Features

- [ ] Streaming SSR.
- [ ] Selective Hydration.
- [ ] Islands Architecture.
- [ ] Incremental Static Regeneration.
- [ ] Advanced DevTools.
- [ ] Plugin System.
- [ ] i18n.
- [ ] Advanced performance optimizations.

**Objetivo:** consolidar o framework como uma plataforma frontend completa.

## Phase 8 — Ecosystem

- [ ] Documentação oficial.
- [ ] Component Library.
- [ ] Spring Boot integration.
- [ ] Testing ecosystem.
- [ ] Benchmark suite.
- [ ] Example applications.
- [ ] Release pipeline.
- [ ] Versioning strategy.
- [ ] Public API stability.

# 26. Architectural Decision Records (ADR)

Toda decisão arquitetural relevante deverá ser documentada.

ADRs aceitos:

| ADR | Título | Status |
|---|---|---|
| [0001](ADR/0001-teavm-as-initial-compiler.md) | TeaVM como compilador inicial | Accepted |
| [0002](ADR/0002-javascript-only-browser-target.md) | JavaScript como único target de navegador | Accepted |
| [0003](ADR/0003-initial-module-boundaries.md) | Fronteiras iniciais dos módulos | Accepted |
| [0004](ADR/0004-dom-interoperability.md) | Estratégia de interoperabilidade com o DOM | Accepted |
| [0005](ADR/0005-build-system-and-versions.md) | Build system, versões e metadados | Accepted |
| [0006](ADR/0006-browser-e2e-testing.md) | Testes E2E de navegador com Playwright Java | Accepted |
| [0007](ADR/0007-first-milestone-scope.md) | Escopo do primeiro milestone: apenas primeiro mount | Accepted |

Decisões futuras (Signals, Component API, Dependency Injection, Hydration,
estratégia de renderização, etc.) ainda não possuem ADR e permanecem propostas
até serem registradas.

Cada ADR deverá registrar:

1. Contexto.
2. Problema.
3. Alternativas consideradas.
4. Decisão.
5. Consequências.
6. Possíveis revisões futuras.

# 27. Engineering Guidelines

## 27.1. Code Quality

- Preferir código fortemente tipado.
- Evitar tipos não tipados.
- Evitar dependências desnecessárias.
- Aplicar SOLID quando apropriado.
- Priorizar composição sobre herança.
- Manter APIs pequenas e consistentes.
- Documentar contratos públicos.

## 27.2. Testing

Toda funcionalidade crítica deverá possuir testes automatizados.

Não considerar uma implementação concluída apenas porque compila.

## 27.3. Performance

Não realizar otimizações baseadas exclusivamente em suposições.

Utilizar benchmarks e profiling.

## 27.4. Agent Development

Agentes de IA deverão respeitar:

- A arquitetura definida neste documento.
- Os contratos públicos dos módulos.
- Os ADRs aprovados.
- As convenções de código.
- Os testes existentes.

Nenhum agente deverá alterar contratos centrais ou introduzir dependências arquiteturais sem revisão.

# 28. Definition of Done

Uma funcionalidade será considerada concluída quando:

1. A implementação estiver funcional.
2. Os testes automatizados passarem.
3. Os contratos públicos estiverem documentados.
4. Não houver regressões conhecidas.
5. Os requisitos de compatibilidade forem atendidos.
6. O comportamento estiver validado no ambiente correspondente.
7. A documentação estiver atualizada.

# 29. Long-Term Vision

O objetivo de longo prazo é transformar o DejaView em um framework frontend Java completo, com capacidade de atender aplicações de diferentes portes.

O framework deverá proporcionar:

- Desenvolvimento Java-first.
- Experiência declarativa.
- Componentização avançada.
- Reatividade granular.
- Compilação para JavaScript.
- CSR, SSR e SSG.
- Ferramentas profissionais.
- Ecossistema extensível.
- Integração com o mundo Java.
- Performance competitiva.

**DejaView — Com certeza você já viu isso antes.**

*But never quite like this.*

---

**Documento de arquitetura — DejaView v0.1.0**
