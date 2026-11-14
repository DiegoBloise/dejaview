package io.dejaview.examples.website.docs;

import java.util.List;

import static io.dejaview.examples.website.docs.Doc.*;

/**
 * Documentação de usuário em português — um tutorial completo, passo a passo.
 * A versão em inglês vive em {@code TutorialEn}; ambas renderizam pelo mesmo modelo.
 */
public final class TutorialPt {

    private TutorialPt() {
    }

    public static List<Block> content() {
        return List.of(

                h2("introduction", "O que você vai construir"),
                prose(text("Este guia leva você de uma máquina vazia a um app DejaView rodando — um contador reativo de verdade no navegador, escrito em Java 25 puro. No caminho você aprende signals, componentes, composição, injeção de dependência, rotas e HTTP. Tudo o que você vê aqui é a mesma API usada pelos exemplos deste repositório.")),
                note("tip", text("Nenhuma ferramenta de build além do Maven Wrapper já incluso no repositório — e você está escrevendo código em menos de cinco minutos.")),

                h2("prerequisites", "1. Pré-requisitos"),
                prose(text("O DejaView tem como alvo o Java 25. Antes de começar, garanta que você tem:")),
                bullets(
                        item(code("JDK 25"), text(" — com o JAVA_HOME apontando para ele (confira com o comando abaixo).")),
                        item(code("Git"), text(" — para clonar o repositório.")),
                        item(text("Acesso à rede no primeiro build, para baixar as dependências do Maven."))),
                shell(Snippets.REQUIREMENTS),
                prose(text("A saída precisa reportar um runtime 25.x. No Windows, use mvnw.cmd no lugar de ./mvnw em todo este guia.")),

                h2("get-the-code", "2. Baixe o código e compile"),
                prose(text("Clone o repositório e rode o build completo:")),
                shell(Snippets.CLONE_BUILD),
                prose(text("O build compila todos os módulos, roda a suíte de testes JVM e compila os exemplos para JavaScript. Quando terminar com BUILD SUCCESS, sua toolchain está funcionando.")),
                note("info", text("./mvnw clean verify também exercita os exemplos compilados em um Chromium headless quando você passa -Pe2e. Isso é opcional e só é necessário para validar comportamento no navegador.")),

                h2("run-the-app", "3. Rode no navegador"),
                prose(text("O exemplo do contador é o menor app DejaView completo. Sirva o resultado compilado com qualquer servidor de arquivos estáticos — por exemplo:")),
                shell(Snippets.SERVE),
                prose(text("Depois abra "), link("http://localhost:4280", "http://localhost:4280"), text(" e clique em Increment. O contador atualiza sem uma única manipulação manual de DOM — é o renderer reativo do DejaView em ação.")),
                prose(text("A saída compilada vive em arquivos estáticos simples:")),
                shell(Snippets.WEBAPP_LAYOUT),
                note("tip", strong("Qualquer servidor estático funciona"), text(" — python3 -m http.server, npx serve, nginx ou a sua hospedagem favorita. Não há servidor de aplicação para rodar em produção.")),

                h2("project-layout", "4. Estrutura do projeto"),
                prose(text("O DejaView é um projeto Maven multi-módulo. Para o dia a dia, você só precisa saber onde o seu código fica:")),
                bullets(
                        item(code("dejaview-core"), text(" — o modelo de views imutável e a API de componentes contra a qual você programa.")),
                        item(code("dejaview-reactivity"), text(" — Signal, Effect e Computed. Sem imports de navegador.")),
                        item(code("dejaview-dom"), text(" — o renderer e o adapter de navegador.")),
                        item(code("dejaview-di"), text(" — injeção de dependência: tokens, providers, escopos, injetores hierárquicos.")),
                        item(code("dejaview-router"), text(" — rotas no cliente com outlets e histórico do navegador.")),
                        item(code("dejaview-http"), text(" — um cliente HTTP tipado com interceptors e cancelamento.")),
                        item(code("examples/counter"), text(" — o app de partida que você buildou acima; o melhor ponto de partida para o seu próprio."))),
                note("info", code("dejaview-core"), text(" nunca importa uma API de navegador. Todo código específico de navegador vive em dejaview-dom, o que permite que os mesmos componentes compilem e sejam testados na JVM.")),

                h2("first-component", "5. Seu primeiro componente"),
                prose(text("Abra "), code("examples/counter/src/main/java/io/dejaview/examples/counter/CounterComponent.java"), text(". Aqui ele está, na íntegra:")),
                java(Snippets.COUNTER_COMPONENT),
                prose(text("Percorra linha por linha:")),
                bullets(
                        item(code("Signal<Integer> count = new Signal<>(0)"), text(" — seu estado reativo, guardado pela instância do componente.")),
                        item(code("render()"), text(" — retorna uma árvore de views. Roda uma vez por montagem e de novo a cada vez que um signal que ela leu muda.")),
                        item(code("Views.p(\"Count: \" + current)"), text(" — view de texto simples; o valor atual do signal é lido durante o render, então o renderer sabe rastreá-lo.")),
                        item(code("onClick(this::increment)"), text(" — binding de evento. increment() chama count.update(v -> v + 1), que agenda exatamente as views que leram count."))),
                prose(text("O ponto de entrada do app monta o componente dentro do elemento com id "), code("app"), text(":")),
                java(Snippets.JAVA_MAIN),

                h2("signals", "6. Estado com signals"),
                prose(text("Signals são o coração do DejaView. get() lê e rastreia, set() substitui o valor, update() o transforma. Computed deriva um novo valor preguiçosamente a partir de outros signals — só recalcula quando é lido depois que uma dependência mudou:")),
                java(Snippets.SIGNALS_AND_COMPUTED),
                prose(text("Effects são a outra metade: tudo dentro de um Effect roda de novo automaticamente quando os signals que ele leu mudam. O renderer usa effects internamente, então as views se atualizam sozinhas — você nunca cutuca o DOM diretamente.")),
                note("tip", strong("Modelo mental:"), text(" um signal é uma célula, render() é uma função dessas células, e o DejaView re-executa exatamente as células que mudaram.")),

                h2("conditional", "7. Renderização condicional"),
                prose(text("Condicionais são apenas Java. Um ternário dentro de render() escolhe entre views; Views.fragment permite retornar múltiplos filhos a partir de um ramo:")),
                java(Snippets.CONDITIONAL),

                h2("lists", "8. Listas e chaves"),
                prose(text("Construa os filhos de uma lista com um loop e dê a cada filho uma chave estável. O DejaView reconcilia filhos com chave por identidade, não por posição — mantendo os nós do DOM (e o estado deles) estáveis quando a lista é reordenada:")),
                java(Snippets.KEYED_LIST),
                note("warning", strong("Sempre dê chave aos filhos da lista."), text(" Sem chave, o DejaView reconcilia por posição — ok para listas append-only, surpreendente em reordenações.")),

                h2("composition", "9. Compondo componentes"),
                prose(text("Componentes se compõem por três mecanismos: inputs, outputs e slots. Aqui está um pai passando dados para baixo, ouvindo eventos e projetando conteúdo em slots nomeados:")),
                java(Snippets.INPUTS),
                prose(text("E o filho recebendo:")),
                java(Snippets.CHILD),
                bullets(
                        item(strong("Inputs"), text(" — definidos via .input(nome, valor) na referência da view do componente; lidos com input(nome, Tipo.class). Inputs ausentes ou com tipo errado falham rápido, com um erro claro.")),
                        item(strong("Outputs"), text(" — o filho chama emit(\"nome\", payload); o pai ouve com .on(\"nome\", handler). Listeners disparam na ordem de registro.")),
                        item(strong("Slots"), text(" — conteúdo padrão é lido com slot(); conteúdo nomeado com slot(\"nome\") e projetado com .slot(\"nome\", view)."))),
                note("info", text("Os valores de input são empurrados pelo renderer a cada atualização, então os filhos veem sempre os valores mais recentes — sem detecção de mudança manual.")),

                h2("lifecycle", "10. Ciclo de vida"),
                prose(text("Três hooks cobrem a vida de uma instância de componente. Sobrescreva apenas os que você precisa:")),
                java(Snippets.LIFECYCLE),
                bullets(
                        item(code("onInit"), text(" — uma vez por nó montado, antes do primeiro render. Bom para configuração única.")),
                        item(code("onUpdate"), text(" — após cada re-render. Bom para logging ou para integrar widgets externos.")),
                        item(code("onDestroy"), text(" — quando o componente é removido. Libere recursos, timers e subscriptions aqui."))),

                h2("di", "11. Injeção de dependência"),
                prose(text("O DejaView traz um injetor pequeno e explícito — sem varredura de classpath e sem reflection. Você mesmo liga tokens a providers:")),
                java(Snippets.DI),
                bullets(
                        item(code("SINGLETON"), text(" é o escopo padrão — uma instância por injetor, compartilhada com seus filhos.")),
                        item(code("TRANSIENT"), text(" cria uma nova instância a cada resolução.")),
                        item(code("SCOPED"), text(" cria uma instância por injetor que resolve — o equivalente a um request scope."))),
                prose(text("Injetores se aninham: um injetor filho enxerga as bindings do pai e pode sombreá-las. Fechar um injetor fecha tudo o que ele criou, na ordem reversa de criação. InjectionToken.of(MinhaClasse.class) dá um token canônico por classe, então bind e inject por tipo sempre concordam.")),

                h2("routing", "12. Rotas"),
                prose(text("Roteamento no cliente é um único módulo, sem fiação obrigatória além de montar um outlet. Defina rotas, entregue o outlet a um componente e o DejaView casa URLs com componentes:")),
                java(Snippets.ROUTER),
                bullets(
                        item(code(":nome"), text(" captura um segmento de caminho; o outlet empurra para o componente casado como um input.")),
                        item(code("**"), text(" é o catch-all — útil para telas iniciais e 404.")),
                        item(text("Guards, redirects, metadata, parâmetros de query e eventos de navegação estão disponíveis no builder da rota."))),
                prose(text("Uma tela parametrizada lê seu input como qualquer outro input:")),
                java(Snippets.ROUTE_PARAMS),
                note("tip", strong("Componha outlets."), text(" Um outlet por região da página é suficiente — aninhe outlets para UIs aninhadas.")),

                h2("http", "13. HTTP"),
                prose(text("O dejaview-http é um cliente HTTP tipado e assíncrono. Requisições são imutáveis, headers são case-insensitive e respostas chegam como valores HttpResult — sucesso ou uma falha estruturada, nunca uma exceção para status não-2xx:")),
                java(Snippets.HTTP_PING),
                bullets(
                        item(text("Entrega exatamente-uma-vez: seu callback é chamado exatamente uma vez por requisição.")),
                        item(text("Interceptors podem observar ou reescrever requisições e respostas — autenticação, logging, tracing.")),
                        item(text("Timeouts, cancelamento e credenciais são opções de primeira classe no builder.")),
                        item(text("JSON é tratado por um leitor/escritor pequeno e sem dependências — sem databind de terceiros."))),

                h2("forms", "14. Formulários reativos"),
                prose(text("O dejaview-forms adiciona controles, grupos e arrays reativos. Um controle é dono do seu valor, do seu estado de validação e das flags de interação, e toda leitura registra uma dependência:")),
                java(Snippets.FORMS_CONTROL),
                prose(text("Validadores são funções simples. Validadores síncronos rodam primeiro; validadores assíncronos só rodam quando o resultado síncrono é válido, e respostas de valores já substituídos são descartadas:")),
                java(Snippets.FORMS_VALIDATION),
                prose(text("Grupos agregam seus filhos: o valor é um mapa imutável, os erros recebem o nome do filho como prefixo e o status sobe pela árvore. Arrays fazem o mesmo com chaves posicionais:")),
                java(Snippets.FORMS_GROUP),
                prose(text("FormBindings conecta controles DOM comuns ao estado do formulário, então digitar, marcar e selecionar atualizam o controle sem listeners manuais:")),
                java(Snippets.FORMS_BINDING),
                bullets(
                        item(code("touched"), text(" — marcado no blur; use para exibir mensagens de validação.")),
                        item(code("dirty"), text(" — marcado na primeira mudança de valor.")),
                        item(code("status()"), text(" — VALID, INVALID, PENDING ou DISABLED, agregado pela árvore."))),
                note("tip", strong("Valores nunca são null."), text(" Use o valor vazio do tipo — \"\", 0 ou false — para um controle não preenchido.")),

                h2("state", "15. Gerenciamento de estado"),
                prose(text("O dejaview-state tira o estado da aplicação de dentro dos componentes individuais. Um StateStore é um holder compartilhado e observável: todo componente que o lê observa o mesmo valor, e escritas são síncronas:")),
                java(Snippets.STATE_STORE),
                prose(text("Estado derivado é um Computed preguiçoso, dono é o store, então ler dentro de um effect registra a projeção. Selector dá à projeção um nome estável para diagnósticos, e Stores.combine deriva valores entre stores independentes — nenhum store global de tudo:")),
                java(Snippets.STATE_DERIVED),
                prose(text("Estado assíncrono é um AsyncState imutável — IDLE, LOADING, SUCCESS ou FAILURE — carregando opcionalmente o último valor bom enquanto um refresh está em voo (stale-while-revalidate). O AsyncStore é dono da operação, um contador de geração descarta tentativas substituídas, e exatamente um resultado vence por tentativa:")),
                java(Snippets.STATE_ASYNC),
                prose(text("Falhas são valores AsyncError estruturados, com um código estável e legível por máquina — aplicações mapeiam códigos para mensagens localizadas, exatamente como as chaves de erro de formulários. Persistência é opcional: PersistedStore restaura o valor antes do primeiro observer existir e depois escreve cada mudança de volta no storage que você fornecer:")),
                java(Snippets.STATE_PERSIST),
                bullets(
                        item(code("PersistedStore"), text(" — restaura do storage antes do primeiro observer existir e escreve de volta cada mudança. Erros de decode e de storage são reportados como códigos, nunca lançados contra você.")),
                        item(code("StateRegistry"), text(" — um objeto comum criado na raiz de composição, com chave por nome; nenhum singleton global.")),
                        item(code("AsyncState.data()"), text(" — o último valor bom, mesmo durante um refresh ou após um refresh falho."))),
                note("tip", strong("Descarte na fronteira."), text(" Um store é dono dos computeds que cria; descarte-os — ou um StateRegistry — quando a view dona for destruída.")),

                h2("cli", "16. A DejaView CLI"),
                prose(text("A CLI é o fluxo de desenvolvimento oficial: cria o projeto, compila e serve com live reload — sem editar arquivos de build à mão:")),
                shell(Snippets.CLI_CREATE),
                prose(text("dejaview new gera um projeto autocontido: o wrapper Maven, um componente contador funcional, a página host e o dejaview.properties. Nada para configurar antes do primeiro build.")),
                prose(text("dejaview run compila o projeto, serve target/webapp em http://localhost:4321, observa os fontes e recompila a cada alteração. A página se recarrega sozinha por meio de um pequeno cliente injetado — você continua escrevendo Java.")),
                bullets(
                        item(code("dejaview build"), text(" — ./mvnw package com diagnóstico acionável quando falha: as linhas [ERROR], com o ruído do Maven filtrado.")),
                        item(code("dejaview run"), text(" — compila e serve target/webapp com live reload; os fontes em src/ são observados e recompilados a cada mudança. Um rebuild que falha continua servindo o último build bom.")),
                        item(code("dejaview doctor"), text(" — valida o JDK, o compilador, o wrapper Maven, a estrutura do projeto e o dejaview.properties."))),
                prose(text("O dejaview.properties controla o servidor de desenvolvimento; flags de linha de comando têm precedência sobre o arquivo:")),
                shell(Snippets.CLI_PROPERTIES),
                note("tip", strong("Usando a CLI a partir do checkout."), text(" Enquanto um lançador standalone não é publicado, compile o módulo e invoque o jar diretamente: java -jar dejaview-cli/target/dejaview-cli-0.1.0-SNAPSHOT.jar <comando>.")),

                h2("styling", "17. Estilizando seu app"),
                prose(text("O DejaView nunca toca no seu CSS. As views carregam atributos class e style simples, então qualquer stylesheet funciona — CSS escrito à mão, Bootstrap, Tailwind CSS, daisyUI:")),
                java(Snippets.STYLING),
                prose(text("Traga o seu framework: adicione o CSS dele no head da página, use as classes dele em .classes(...), e pronto. O DejaView não entrega kit de UI nem pipeline de CSS, de propósito.")),

                h2("production", "18. Build de produção"),
                prose(text("Dois perfis de build compartilham o mesmo pipeline. O build de desenvolvimento padrão usa otimização SIMPLE e JavaScript legível, sem minificação — mais fácil de depurar. O perfil de produção ajusta os mesmos parâmetros para os valores otimizados com uma única flag:")),
                shell(Snippets.PRODUCTION_BUILD),
                bullets(
                        item(code("optimizationLevel=FULL"), text(" — o dead-code elimination do TeaVM remove tudo que o bundle não alcança.")),
                        item(code("minify"), text(" — o JavaScript emitido é minificado: identificadores curtos e espaços colapsados.")),
                        item(code("app.js.map"), text(" — o source map é preservado para diagnósticos de campo e não custa nada em runtime."))),
                prose(text("Todo build também roda o dejaview-compiler sobre o webapp montado antes que ele possa ir para produção: ele falha em assets ausentes ou vazios — reportando cada problema junto com o arquivo que o referencia — e gera a impressão digital SHA-256 da saída:")),
                shell(Snippets.WEBAPP_LAYOUT),
                bullets(
                        item(code("app.js"), text(" — sua aplicação, compilada.")),
                        item(code("app.js.map"), text(" — o source map para debugging.")),
                        item(code("dejaview-assets.json"), text(" — todos os arquivos do webapp com tamanho e SHA-256; gerado pelo build, determinístico entre builds equivalentes.")),
                        item(code("index.html"), text(" — a página de entrada que carrega o bundle."))),
                prose(text("A saída é reprodutível: fontes equivalentes sob condições equivalentes produzem JavaScript e manifestos byte-idênticos — arquivos compactados incluídos. Suba esses arquivos para qualquer hospedagem estática (nginx, S3, Netlify, GitHub Pages) e você tem um deploy de produção.")),

                h2("server", "19. Plataforma servidor (alpha)"),
                prose(text("O dejaview-server é a base para server-side rendering: ele executa árvores de componentes na JVM — sem navegador, sem DOM — e produz uma descrição neutra de plataforma da saída. Adicione-o à sua aplicação para experimentar execução no servidor hoje; a serialização para HTML construída sobre ele chega no dejaview-ssr, coberto na próxima seção.")),
                java(Snippets.SERVER_PLATFORM),
                bullets(
                        item(code("ServerContext"), text(" — um por requisição: um injetor filho que vincula REQUEST/CONTEXT apenas dentro do escopo da requisição, e um MemoryStateStorage fresco por requisição (a menos que você vincule o seu próprio), para que requisições nunca vejam o estado umas das outras.")),
                        item(code("ServerRenderer.render(view, context)"), text(" — percorre sua árvore de Views e retorna um ServerTree: nós ServerElement, ServerText e ServerFragment, com os handlers de eventos de elementos descartados por design e reportados em tree.droppedEvents().")),
                        item(code("BrowserOnlyComponent"), text(" — implemente este marker no componente (ou registre-o em context.compatibility()) e o servidor rejeita o componente com browser-only-component antes que bind/onInit/render sejam executados — falha rápida, nunca um null pointer obscuro no meio de código de navegador.")),
                        item(code("SerializationBoundary.write(value)"), text(" — o gateway JSON estrito: null, árvores JsonValue, strings, booleans, números finitos, mapas com chaves string e listas passam; qualquer outra coisa falha rápido com not-serializable."))),
                prose(text("Componentes no servidor passam pelo mesmo pipeline de montagem do navegador — criados uma vez por nó montado, vinculados, inicializados, renderizados uma vez — e são destruídos e fechados em ordem reversa quando a renderização termina ou falha. Seus componentes não mudam; apenas a plataforma é diferente.")),
                note("info", strong("Cedo e propositalmente estreito."), text(" A saída HTML agora chega no dejaview-ssr (próxima seção); hydration, streaming progressivo e integração com servlet continuam como marcos futuros. Handlers de eventos nunca cruzam a fronteira. A execução server-only é um renderer, não um runtime: o dejaview-server não contém APIs de navegador nem imports de TeaVM.")),

                h2("ssr", "20. Renderização no servidor (alpha)"),
                prose(text("O dejaview-ssr transforma a mesma árvore de componentes em um documento HTML completo na JVM. Um componente lê a requisição pelos inputs e escreve os metadados da página enquanto renderiza; o pipeline escapa cada valor, monta o documento e entrega ao host um SsrResult pronto para enviar:")),
                java(Snippets.SSR_PAGE),
                prose(text("O host cria um PageMeta e um ServerContext com escopo de requisição por requisição. renderPage retorna o status, o HTML, os eventos descartados e quaisquer falhas de boundary ou avisos de validação:")),
                java(Snippets.SSR_HOST),
                bullets(
                        item(code("SsrRenderer.renderPage"), text(" — monta a view com o ServerRenderer, valida a árvore e retorna um SsrResult. Uma falha não tratada de render ou validação vira uma página 500 genérica com a exceção anexada para log — nunca vazada para o navegador.")),
                        item(code("PageMeta"), text(" — lang, title, description, URL canônica e tags meta/Open Graph por requisição. Os componentes escrevem nele durante o render; o head é montado depois.")),
                        item(code("ServerErrorBoundary"), text(" — um marker de componente: quando a subárvore lança, os componentes que falharam são destruídos e fechados, a falha é reportada em boundaryErrors e fallback(error) é renderizado no lugar.")),
                        item(code("SsrValidator"), text(" — rejeita nomes de tag ou atributo ilegais, elementos void com filhos e árvores com mais de 512 níveis com ssr-validation-failed. Texto e valores de atributos são sempre escapados, então dados da aplicação não podem virar markup."))),
                prose(text("Um error boundary mantém a página útil quando um widget falha:")),
                java(Snippets.SSR_BOUNDARY),
                prose(text("Rode o exemplo incluído e abra com JavaScript desabilitado — links e o formulário GET de busca continuam funcionando:")),
                shell(Snippets.SSR_RUN),
                note("warning", strong("Alpha e propositalmente estreito."), text(" O resultado é uma página completa, não um aplicativo hidratado: serialização de estado e hydration são marcos futuros, e as variantes com Writer renderizam e validam uma vez em vez de transmitir progressivamente. Apenas as propriedades DOM value e booleanas são serializadas; as demais são omitidas e reportadas como warnings. O dejaview-ssr é código JVM puro — sem APIs de navegador, sem imports de TeaVM — e texto dentro de elementos script ou style é escapado em vez de executado.")),

                h2("ssg", "21. Geração de site estático (alpha)"),
                prose(text("O dejaview-ssg transforma o pipeline de SSR em uma etapa de build: ele renderiza cada rota declarada em um arquivo, copia assets, valida se cada href e src local resolve para um arquivo gerado e emite um sitemap. O resultado é um diretório que você sobe para GitHub Pages, S3, Netlify ou nginx — sem servidor, sem JavaScript, sem runtime.")),
                prose(text("As rotas são declaradas explicitamente em Java, porque o framework não tem reflection nem scanning de anotações. Uma página estática é criada com SsgRoute.page; uma rota dinâmica é criada com SsgRoute.parametrized, que expande um placeholder {name} por conjunto de parâmetros que a aplicação fornece no build:")),
                java(Snippets.SSG_ROUTE),
                prose(text("O gerador renderiza cada página com seu próprio injetor e ServerContext com escopo de requisição, então rotas não podem vazar estado umas para as outras. Todo caminho e valor de parâmetro é validado antes de virar nome de arquivo, caminhos duplicados falham o build, e o diretório de saída é limpo por padrão para que páginas antigas nunca sobrevivam a um build:")),
                java(Snippets.SSG_SITE),
                bullets(
                        item(code("SsgGenerator.generate"), text(" — planeja rotas, renderiza páginas, copia assets, escreve o sitemap e verifica referências locais. Uma falha nomeia a rota ou o asset que a causou.")),
                        item(code("SsgRoute"), text(" — page(path, lang, view) para uma página estática; parametrized(pattern, lang, parameterSets, view) para uma dinâmica. Caminhos devem ser segmentos seguros; valores de parâmetros não podem conter '..', '/', espaços ou travessia percent-encoded.")),
                        item(code("SsgAsset"), text(" — copia um arquivo ou diretório para o site. Um diretório é copiado recursivamente, mantendo seu layout interno.")),
                        item(code("SsgSite"), text(" — a descrição do site: diretório de saída, rotas, assets, URL base opcional (habilita links canônicos e sitemap.xml) e a flag clean."))),
                prose(text("Páginas geradas carregam os metadados que seus componentes escreveram — lang, title, description, canonical e tags meta/Open Graph — e cada referência local é verificada contra os arquivos escritos, então um link quebrado falha o build em vez de publicar um 404 em produção:")),
                shell(Snippets.SSG_RUN),
                note("warning", strong("Alpha e propositalmente estreito."), text(" A saída é uma página completa, não um aplicativo hidratado: serialização de estado e hydration são marcos futuros. Páginas geradas são escritas uma vez, não transmitidas em streaming, e apenas o subconjunto de propriedades do SSR é representável. O gerador é JVM-only — sem APIs de navegador, sem imports de TeaVM — e roda no build, não no navegador.")),

                h2("troubleshooting", "22. Problemas comuns"),
                bullets(
                        item(code("java -version mostra 8, 17 ou 21"), text(" — seu JAVA_HOME aponta para o JDK errado. Aponte para uma instalação do Java 25 e reabra o terminal.")),
                        item(code("permission denied: ./mvnw"), text(" — rode chmod +x mvnw, ou use mvnw.cmd no Windows.")),
                        item(code("Porta 4280 já em uso"), text(" — escolha outra porta: python3 -m http.server 4281 -d examples/counter/target/webapp.")),
                        item(code("Página em branco, sem contador"), text(" — abra o console do navegador. Um 404 para app.js significa que você está servindo o diretório errado; precisa ser examples/counter/target/webapp, não a raiz do repositório.")),
                        item(code("BUILD FAILURE baixando dependências"), text(" — confira o acesso à rede; o Maven precisa alcançar repo.maven.apache.org no primeiro build."))),

                h2("next-steps", "23. Próximos passos"),
                prose(
                        text("Explore o "),
                        link("/website/pt/playground/", "playground interativo"),
                        text(" — cada exemplo roda o framework compilado de verdade no seu navegador — e depois construa o seu app a partir do exemplo do contador. O código-fonte completo, issues e discussões vivem no "),
                        link("https://github.com/dejaview/dejaview", "GitHub"),
                        text(".")));
    }
}
