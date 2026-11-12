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

                h2("styling", "14. Estilizando seu app"),
                prose(text("O DejaView nunca toca no seu CSS. As views carregam atributos class e style simples, então qualquer stylesheet funciona — CSS escrito à mão, Bootstrap, Tailwind CSS, daisyUI:")),
                java(Snippets.STYLING),
                prose(text("Traga o seu framework: adicione o CSS dele no head da página, use as classes dele em .classes(...), e pronto. O DejaView não entrega kit de UI nem pipeline de CSS, de propósito.")),

                h2("production", "15. Build de produção"),
                prose(text("O build que você rodou no passo 2 já é o de produção. ./mvnw clean verify compila tudo para JavaScript e escreve o resultado no diretório target/webapp de cada exemplo — arquivos simples que você pode hospedar em qualquer lugar:")),
                shell(Snippets.WEBAPP_LAYOUT),
                bullets(
                        item(code("app.js"), text(" — sua aplicação, compilada.")),
                        item(code("app.js.map"), text(" — o source map para debugging.")),
                        item(code("index.html"), text(" — a página de entrada que carrega o bundle."))),
                prose(text("Suba esses arquivos para qualquer hospedagem estática (nginx, S3, Netlify, GitHub Pages) e você tem um deploy de produção.")),

                h2("troubleshooting", "16. Problemas comuns"),
                bullets(
                        item(code("java -version mostra 8, 17 ou 21"), text(" — seu JAVA_HOME aponta para o JDK errado. Aponte para uma instalação do Java 25 e reabra o terminal.")),
                        item(code("permission denied: ./mvnw"), text(" — rode chmod +x mvnw, ou use mvnw.cmd no Windows.")),
                        item(code("Porta 4280 já em uso"), text(" — escolha outra porta: python3 -m http.server 4281 -d examples/counter/target/webapp.")),
                        item(code("Página em branco, sem contador"), text(" — abra o console do navegador. Um 404 para app.js significa que você está servindo o diretório errado; precisa ser examples/counter/target/webapp, não a raiz do repositório.")),
                        item(code("BUILD FAILURE baixando dependências"), text(" — confira o acesso à rede; o Maven precisa alcançar repo.maven.apache.org no primeiro build."))),

                h2("next-steps", "17. Próximos passos"),
                prose(
                        text("Explore o "),
                        link("/website/pt/playground/", "playground interativo"),
                        text(" — cada exemplo roda o framework compilado de verdade no seu navegador — e depois construa o seu app a partir do exemplo do contador. O código-fonte completo, issues e discussões vivem no "),
                        link("https://github.com/dejaview/dejaview", "GitHub"),
                        text(".")));
    }
}
