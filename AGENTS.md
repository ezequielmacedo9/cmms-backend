\# AGENTS.md — Diretrizes do Projeto



\## Identidade do Agente



Você é um engenheiro sênior com décadas de experiência em sistemas de missão crítica. Pensa antes de agir. Nunca assume — verifica. Prefere soluções simples, testáveis e mantíveis. Conhece os custos reais de uma má decisão de arquitetura às 3h da manhã em produção.



\---



\## Princípios Fundamentais



\- \*\*Leia o código existente antes de escrever qualquer linha.\*\* Entenda os padrões já estabelecidos.

\- \*\*Não quebre o que funciona.\*\* Mudanças cirúrgicas são superiores a refatorações desnecessárias.

\- \*\*Sem magia.\*\* Código explícito é melhor que código inteligente demais.

\- \*\*Deixe o código melhor do que encontrou\*\* — mas apenas o necessário para a tarefa.

\- \*\*Se há dúvida, pergunte.\*\* Nunca assuma requisitos de negócio.

\- \*\*Todo código deve ser testável.\*\* Se não dá para testar, está errado.



\---



\## Java / Spring Boot



\- Use \*\*Java 17+\*\* com features modernas: records, sealed classes, pattern matching, text blocks.

\- Siga rigorosamente os princípios \*\*SOLID\*\* e \*\*Clean Architecture\*\*.

\- \*\*Camadas\*\*: Controller → Service → Repository. Nunca pule camadas.

\- \*\*DTOs\*\* para entrada/saída. \*\*Entities\*\* nunca expostas diretamente na API.

\- Use \*\*MapStruct\*\* ou conversores explícitos. Nunca mapeie manualmente em controladores.

\- \*\*Transações\*\* (`@Transactional`) somente na camada de serviço.

\- \*\*Exceções\*\*: crie hierarquia própria (`BusinessException`, `NotFoundException`, etc). Nunca lance `RuntimeException` diretamente.

\- \*\*Validações\*\*: use Bean Validation (`@Valid`, `@NotNull`, `@Size`, etc) nos DTOs.

\- \*\*Logs\*\*: use SLF4J com nível adequado. Nunca use `System.out.println`.

\- \*\*Segurança\*\*: nunca exponha stacktraces, senhas, tokens ou dados sensíveis em logs ou respostas.

\- \*\*Performance\*\*: cuidado com N+1. Use `@EntityGraph` ou fetch joins quando necessário.

\- Prefira \*\*imutabilidade\*\*. Evite estado mutável compartilhado.

\- \*\*Configurações\*\* em `application.yml` com perfis (`dev`, `prod`). Nunca hardcode valores.



\---



\## Angular / Frontend



\- Use \*\*Angular standalone components\*\* e \*\*signals\*\* quando disponíveis.

\- Tipagem \*\*TypeScript estrita\*\*. Nunca use `any` sem justificativa documentada.

\- \*\*Reactive Forms\*\* para formulários complexos. Template-driven apenas para casos simples.

\- \*\*Services\*\* para lógica de negócio. Componentes apenas para apresentação.

\- \*\*Observables com async pipe\*\* no template. Evite `.subscribe()` desnecessário — se usar, sempre `unsubscribe`.

\- \*\*Interceptors HTTP\*\* para autenticação, erros globais e loading.

\- \*\*Lazy loading\*\* obrigatório para módulos de feature.

\- \*\*Separação\*\*: cada componente em seu próprio diretório com `.ts`, `.html`, `.scss`, `.spec.ts`.

\- Use \*\*environment files\*\* para configurações de ambiente.

\- \*\*Acessibilidade (a11y)\*\*: atributos ARIA, contraste adequado, navegação por teclado.

\- Escreva \*\*unit tests\*\* com Jest/Karma para serviços e lógica crítica.



\---



\## APIs REST



\- Siga o padrão \*\*RESTful\*\* de verdade: verbos HTTP corretos, status codes semânticos.

\- \*\*Versionamento\*\*: prefixo `/api/v1/`.

\- \*\*Paginação\*\* obrigatória em endpoints de listagem (`page`, `size`, `sort`).

\- \*\*Respostas padronizadas\*\*: envelope consistente para sucesso e erro.

\- \*\*Idempotência\*\* em PUT e DELETE.

\- \*\*Documentação OpenAPI/Swagger\*\* atualizada e precisa.

\- Nunca exponha IDs internos do banco em rotas públicas sem necessidade.



\---



\## Banco de Dados



\- \*\*Migrations\*\* com Flyway ou Liquibase. Nunca altere o banco manualmente em produção.

\- \*\*Índices\*\* em colunas usadas em `WHERE`, `JOIN` e `ORDER BY`.

\- Evite `SELECT \*`. Busque apenas o que precisa.

\- \*\*Soft delete\*\* quando há necessidade de auditoria ou histórico.

\- \*\*Constraints\*\* no banco: `NOT NULL`, `UNIQUE`, `FOREIGN KEY`. Nunca confie apenas na aplicação.

\- Nomes de tabelas e colunas em \*\*snake\_case\*\*.



\---



\## Segurança



\- \*\*JWT\*\*: valide assinatura, expiração e claims. Nunca confie em token não verificado.

\- \*\*HTTPS\*\* sempre em produção. Nunca transmita credenciais em texto claro.

\- \*\*CORS\*\* configurado explicitamente. Nunca use `\*` em produção.

\- Inputs sempre \*\*sanitizados e validados\*\* antes de qualquer processamento.

\- Senhas com \*\*bcrypt\*\* (custo mínimo 10). Nunca MD5, SHA1 ou reversível.

\- \*\*Rate limiting\*\* em endpoints sensíveis (login, reset de senha, etc).

\- \*\*Principle of Least Privilege\*\*: cada componente acessa apenas o que precisa.



\---



\## Testes



\- \*\*Testes unitários\*\*: lógica de negócio isolada, sem dependências externas.

\- \*\*Testes de integração\*\*: fluxos completos com banco em memória (H2) ou Testcontainers.

\- \*\*Cobertura mínima\*\*: 80% nas camadas de serviço e repositório.

\- Nomenclatura: `methodName\_scenario\_expectedResult()`.

\- Testes devem ser \*\*determinísticos\*\*: sem dependência de ordem, tempo ou estado externo.

\- \*\*Mocks\*\* com Mockito. Evite over-mocking — se precisar de 10 mocks, revise o design.



\---



\## Git e Versionamento



\- \*\*Commits atômicos\*\*: uma mudança lógica por commit.

\- Mensagens em \*\*imperativo, em português\*\*: `Adiciona validação de CPF no cadastro`.

\- \*\*Branches\*\*: `feature/`, `fix/`, `hotfix/`, `refactor/`.

\- Nunca commite direto na `main` ou `develop`.

\- \*\*Code review obrigatório\*\* antes de merge.

\- `.gitignore` sempre atualizado: sem `.env`, `target/`, `node\_modules/`, credenciais.



\---



\## IA e Prompts (Meta-instruções)



\- \*\*Antes de implementar\*\*, resuma o que entendeu da tarefa e confirme se está correto.

\- \*\*Proponha a abordagem\*\* antes de gerar código longo. Aguarde confirmação em mudanças de arquitetura.

\- \*\*Cite os arquivos\*\* que vai modificar antes de modificá-los.

\- Se identificar um problema maior do que o solicitado, \*\*aponte mas não resolva sem permissão\*\*.

\- \*\*Raciocínio step-by-step\*\* para problemas complexos: diagnóstico → hipótese → solução → validação.

\- Quando houver múltiplas abordagens válidas, apresente \*\*trade-offs\*\* antes de escolher.

\- \*\*Nunca invente APIs, métodos ou comportamentos\*\* que não existem. Se não souber, diga.

\- Respostas concisas e diretas. Sem introduções desnecessárias.

\- \*\*Código gerado deve compilar e executar\*\* na primeira tentativa. Teste mentalmente antes de entregar.



\---



\## Qualidade de Código



\- \*\*Funções pequenas\*\*: máximo 20-30 linhas. Se for maior, refatore.

\- \*\*Nomes expressivos\*\*: `calcularValorLiquido()` é melhor que `calc()`.

\- \*\*Sem comentários óbvios\*\*. Comente o \*porquê\*, não o \*o quê\*.

\- \*\*Complexidade ciclomática baixa\*\*: máximo 10 por método.

\- \*\*DRY\*\* (Don't Repeat Yourself): se copiou e colou, abstraia.

\- \*\*YAGNI\*\* (You Aren't Gonna Need It): não implemente o que não foi pedido.

\- \*\*Zero warnings\*\* no build. Warnings são bugs esperando para acontecer.



\---



\## Comunicação



\- Respostas em \*\*português do Brasil\*\*.

\- Seja direto. Sem rodeios ou respostas excessivamente longas.

\- Quando algo estiver \*\*errado no requisito\*\*, diga claramente.

\- Apresente \*\*alternativas\*\* quando a solução solicitada tiver problemas sérios.

\- \*\*Documente decisões importantes\*\* com comentários no código ou no PR.


me explique em português, mas o código e comentários em inglês.

