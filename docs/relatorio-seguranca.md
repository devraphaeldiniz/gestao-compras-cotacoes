# Relatório de teste de vulnerabilidade

| | |
|---|---|
| **Aplicação** | Sistema de Gestão de Compras e Cotações (API REST + interface web) |
| **Versão avaliada** | commit `d6b8a7a` (Spring Boot 3.4.5), rodando em Docker local |
| **Data** | 26/11/2026 |
| **Tipo** | Caixa-branca: código-fonte, dependências, imagem Docker e aplicação em execução |
| **Resultado** | 11 achados, todos corrigidos e cobertos por teste de regressão (SEC-00, autenticação, resolvido na versão Spring Boot 4) |

## Resumo

| Severidade | Encontrados | Corrigidos | Em aberto |
|---|---|---|---|
| Crítica | 1 (dependências) | 1 | 0 |
| Alta | 3 | 3 | 0 |
| Média | 3 | 3 | 0 |
| Baixa | 4 | 4 | 0 |

Vulnerabilidades conhecidas (CVE) na imagem, pelo Trivy: **89 antes → 2 depois → 0 após a migração para o Spring Boot 4** (as 2 que restavam estão em [Riscos aceitos](#riscos-aceitos), hoje encerrados).

## Metodologia e ferramentas

| Camada | Ferramenta | Escopo |
|---|---|---|
| SAST (código) | Semgrep (`p/java`, `p/owasp-top-ten`, `p/secrets`, `p/dockerfile`) | Todo o repositório |
| SCA (dependências) | Trivy 0.58 (`image` e `fs`) | JARs da imagem, `pom.xml`, pacotes Alpine, segredos e misconfig |
| DAST (aplicação rodando) | OWASP ZAP (`zap-api-scan`, scan ativo guiado pelo OpenAPI) | Todas as rotas `/api/**` documentadas |
| Testes manuais | `curl` e requisições paralelas | Concorrência, limites numéricos, métodos e content-types, injeção, XSS armazenado, actuator, CORS, cabeçalhos, tamanho do corpo |

## Achados

### SEC-00 · Alta · Ausência de autenticação e autorização · Corrigido

Qualquer cliente com acesso à rede pode cadastrar fornecedores, decidir cotações e receber pedidos; não há identidade do comprador nem trilha de quem decidiu. É o maior risco para uso real e é uma decisão de produto (perfis, provedor de identidade), por isso não foi implementado neste ciclo.

**Correção:** Spring Security com login próprio emitindo JWT HS256 (`POST /api/auth/login`, validado pelo OAuth2 Resource Server) e três perfis:

- `COMPRADOR`: cadastros, estoque, cotações e propostas;
- `APROVADOR`: decisão, recebimento e cancelamento de pedido;
- `ADMIN`: tudo, além de usuários e métricas.

Comprador não decide (segregação de funções). A decisão, o recebimento, os cancelamentos e cada movimentação de estoque gravam o usuário: colunas `decidido_por`/`emitido_por`/`cancelado_por`, histórico de eventos e extrato.

- **Senhas:** BCrypt.
- **Força bruta:** 5 falhas bloqueiam o usuário por 15 minutos (`429` com `Retry-After`). Usuário inexistente gasta o mesmo tempo de BCrypt, então não dá para enumerar logins.
- **Respostas 401/403:** saem em JSON, no idioma da requisição.
- **Segredo do JWT:** obrigatório com 32 ou mais caracteres. Sem ele, a chave é aleatória a cada subida (só para desenvolvimento).
- **Actuator:** `/actuator/prometheus` exige ADMIN na porta pública.

**Regressão:** `AutenticacaoIntegracaoTest` (401 sem token, permissões por perfil, login real, bloqueio, usuário inativo) e `ProtecaoForcaBrutaTest`.

### SEC-01 · Alta · Perda de atualização de estoque sob concorrência · Corrigido

**Evidência:** 20 saídas simultâneas de 1 unidade num produto com saldo 40 terminaram com saldo **23** (esperado 20): três baixas se perderam. A mesma corrida permitia, em tese, gerar duas cotações de reposição para o mesmo produto.

**Correção:** bloqueio pessimista (`SELECT ... FOR UPDATE`) do produto em toda movimentação e recebimento (`ProdutoRepository.buscarComBloqueio`); índice único parcial no banco garantindo no máximo uma cotação `RASCUNHO`/`ABERTA` por produto (`V3__uma_cotacao_em_andamento_por_produto.sql`).
**Regressão:** `SegurancaIntegracaoTest` (20 saídas paralelas → saldo 20; 12 saídas paralelas → uma única cotação).

### SEC-02 · Alta · Recebimento duplicado do mesmo pedido · Corrigido

**Evidência:** 10 chamadas paralelas a `POST /api/pedidos-compra/1/receber` retornaram **2 × 200**: as duas transações passaram pela checagem de status `EMITIDO` antes de qualquer uma gravar.

**Correção:** bloqueio da linha do pedido e, em seguida, do produto (ordem fixa, sem deadlock); a cotação também é bloqueada na abertura, no registro de proposta e na decisão, impedindo duas decisões simultâneas ou uma proposta entrando no meio de uma decisão.
**Regressão:** 10 recebimentos paralelos → exatamente 1 × 200 e 9 × 422, estoque somado uma vez.

### SEC-03 · Crítica · Dependências com vulnerabilidades conhecidas · Corrigido

**Evidência (Trivy):** 89 CVEs na imagem, sendo 9 críticas e 32 altas, em Tomcat 10.1.40, Spring Framework 6.2.6, Jackson 2.18.3, pgjdbc 42.7.5, Spring Boot 3.4.5, logback, commons-lang3 e pacotes Alpine.

**Correção:** Spring Boot 3.5.16, Tomcat 10.1.62, Jackson 2.21.9, pgjdbc 42.7.15, commons-lang3 3.22.0, log4j-api 2.25.5, springdoc 2.10.1 (todas na mesma linha de versão, sem troca de major) e `apk upgrade` na imagem. Dependabot semanal (Maven, Docker, Actions) e job de Trivy no CI que falha com vulnerabilidade crítica corrigível.

### SEC-04 · Média · Overflow e truncamento silencioso de valores numéricos · Corrigido

**Evidência:** `saldoAtual: 1e20` era aceito pela API e estourava a coluna `NUMERIC(19,4)`, voltando **409** com mensagem de "conflito de integridade" (enganosa); `1.123456789` era gravado como `1.1235` sem aviso, enquanto a resposta mostrava o valor original.

**Correção:** `@Digits` em todas as quantidades e valores (alinhado à precisão de cada coluna), `@Max(3650)` nos prazos e checagem do total calculado da proposta contra o limite de `NUMERIC(19,2)`. Agora a resposta é **400** com o campo apontado.

### SEC-05 · Média · Cabeçalhos de segurança ausentes · Corrigido

**Evidência:** nenhuma resposta trazia `X-Content-Type-Options`, `X-Frame-Options`, CSP ou `Cross-Origin-Resource-Policy` (ZAP 10021 e 90004); respostas da API eram cacheáveis (ZAP 10049).

**Correção:** filtro `CabecalhosSegurancaFiltro` com `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`, COOP/CORP e **CSP estrita sem `'unsafe-inline'`** (o JavaScript e o CSS da interface foram movidos para `app.js` e `app.css`); `Cache-Control: no-store` em `/api/**`.

### SEC-06 · Média · Pacotes da imagem base desatualizados · Corrigido

**Evidência:** libexpat 2.10.0 e zlib 1.3.2 com CVEs médias na `eclipse-temurin:21-jre-alpine`. **Correção:** `apk upgrade --no-cache` no estágio de runtime.

### SEC-07 · Baixa · Sem limite de tamanho do corpo · Corrigido

**Evidência:** um JSON de 5 MB era lido inteiro em memória antes de ser recusado pela validação. **Correção:** corpo acima de 1 MB em `/api/**` é recusado com **413** no filtro, antes do parse. Requisições `chunked` sem `Content-Length` devem ser limitadas também no proxy reverso.

### SEC-08 · Baixa · Método ou content-type errado devolvia 500 · Corrigido

**Evidência:** `DELETE /api/produtos`, `GET` em rota só `PATCH` e `POST` com `text/plain` respondiam **500 Internal Server Error**. **Correção:** tratamento explícito no `GlobalExceptionHandler`: **405** (com header `Allow`), **415** e **406**, com mensagens localizadas.

### SEC-09 · Baixa · Superfície de administração exposta · Corrigido

**Evidência:** `/actuator` (página de descoberta) e `/actuator/info` públicos; Swagger sempre ligado; `server.error.include-message: always`. **Correção:** só `/actuator/health` exposto, sem detalhes e sem descoberta; Swagger desligável com `SWAGGER_ENABLED=false`; `/error` sem mensagem, exceção ou stack trace.

### SEC-10 · Baixa · Actions do CI referenciadas por tag mutável · Corrigido

**Evidência (Semgrep `github-actions-mutable-action-tag`):** `actions/checkout@v4` e afins podem ser reapontados. **Correção:** actions fixadas por SHA de commit (com a versão em comentário), imagem do Trivy fixada por digest e Dependabot para mantê-las atualizadas.

## Controles verificados sem achado

| Teste | Resultado |
|---|---|
| SQL injection em filtros e caminhos (`?status=ABERTA' OR 1=1--`, `?ativo=true'or'1'='1`, ids não numéricos) | 400; o JPA usa consultas parametrizadas e os filtros são enums, booleanos e longs |
| XSS armazenado (`<img src=x onerror=...><script>` no nome do produto) | Exibido como texto; o front escapa todo conteúdo e a CSP bloquearia script inline |
| CORS (preflight de origem externa) | 403, sem `Access-Control-Allow-Origin` |
| `TRACE` | 405 |
| Vazamento de erro | Nenhuma resposta traz stack trace, SQL ou nome de classe (ZAP 90022 passou) |
| Segredos no repositório | Nenhum (Trivy `secret`, Semgrep `p/secrets`) |
| Injeções do ZAP (SQLi, OS command, SSTI, XXE, XPath, CRLF, path traversal etc.) | 120 de 121 regras passaram após as correções; o aviso restante é o `/` devolvendo HTML (a interface web) num scan de API |
| Container | Roda como usuário não-root (`app`) |

## Riscos aceitos

| CVE | Componente | Por que não se aplica |
|---|---|---|
| CVE-2026-47884 (Crítica) | spring-webmvc 6.2.19 | Exige `XsltView` com mapeamento `/**` renderizando views. A aplicação só tem `@RestController` com JSON; não há `XsltView`, `ModelAndView` nem templates no código. |
| CVE-2026-47890 (Crítica) | spring-webmvc 6.2.19 | Exige Server-Sent Events com view fragments. A aplicação não usa SSE. |

As duas só tinham correção na linha Spring Framework 7.x. **Encerrado:** a aplicação migrou para o Spring Boot 4.2 (Spring Framework 7.1). O Trivy na imagem nova não encontra nenhuma vulnerabilidade, de nenhuma severidade, e o `.trivyignore` ficou vazio.

## Pendências recomendadas

1. ~~Autenticação e autorização (SEC-00)~~: feito (JWT + perfis).
2. ~~Migração para Spring Boot 4~~: feita; riscos aceitos encerrados.
3. Em produção: `APP_JWT_SEGREDO` forte vindo de um cofre de segredos, sem carregar `db/seed` (usuários de demonstração), credenciais do banco via secret (não os padrões do `docker-compose.yml`), TLS no proxy, `SWAGGER_ENABLED=false` e limite de corpo e de taxa no gateway.

## Como reproduzir

```bash
# Testes de regressão de segurança (PostgreSQL real via Testcontainers)
./mvnw test -Dtest=SegurancaIntegracaoTest

# Dependências e imagem (igual ao CI)
docker compose --profile app up -d --build
docker run --rm -v /var/run/docker.sock:/var/run/docker.sock -v "$PWD/.trivyignore:/.trivyignore:ro" \
  aquasec/trivy:latest image --severity CRITICAL --ignore-unfixed --ignorefile /.trivyignore compras-cotacoes-app

# Código
docker run --rm -v "$PWD:/src" semgrep/semgrep semgrep scan --config p/java --config p/owasp-top-ten --config p/secrets /src

# Aplicação em execução (DAST)
docker run --rm zaproxy/zap-stable zap-api-scan.py -t http://host.docker.internal:8080/v3/api-docs -f openapi
```
