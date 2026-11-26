package com.compras.cotacoes.e2e;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * E2E da interface web num Chromium headless contra a aplicação real (porta aleatória + PostgreSQL do Testcontainers
 * com os dados de demonstração). Roda só com {@code ./mvnw test -Pe2e}. Também garante que a página não gera erro
 * de console, o que pega violações da CSP (script/estilo inline bloqueado).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.flyway.locations=classpath:db/migration,classpath:db/seed",
        "app.prazos.habilitado=false",
        "app.email.habilitado=false"})
class InterfaceE2E {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int porta;

    private BrowserContext contexto;
    private Page page;
    private final List<String> errosConsole = new ArrayList<>();

    @BeforeAll
    static void abrirNavegador() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void fecharNavegador() {
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void novaAba() {
        contexto = browser.newContext(new Browser.NewContextOptions().setLocale("pt-BR").setViewportSize(1440, 900));
        page = contexto.newPage();
        page.onConsoleMessage(m -> {
            if ("error".equals(m.type())) {
                errosConsole.add(m.text());
            }
        });
        page.onPageError(errosConsole::add);
        page.navigate("http://localhost:" + porta + "/");
    }

    @AfterEach
    void fecharAba() {
        contexto.close();
    }

    @Test
    void ciclo_comprador_cota_e_aprovador_decide_e_recebe_parcialmente() {
        entrar("comprador", "comprador123");
        assertThat(page.locator("#usuario-perfil")).hasText("COMPRADOR");
        assertThat(page.locator("[data-aba=usuarios]")).isHidden();
        assertThat(page.locator("#tb-produtos")).containsText("PAP-A4-500");

        // Comprador vê a cotação aberta e pode registrar proposta, mas não decidir
        abrirCotacao("COT-2026-00001");
        assertThat(page.locator("#form-proposta")).isVisible();
        assertThat(page.locator("#form-decidir")).hasCount(0);
        assertThat(page.locator("#detalhe")).containsText("Distribuidora Norte S.A.");

        // Aprovador decide pela pontuação ponderada
        page.locator("#sair").click();
        entrar("aprovador", "aprovador123");
        abrirCotacao("COT-2026-00001");
        page.locator("#form-decidir select[name=criterio]").selectOption("PONTUACAO_PONDERADA");
        page.locator("#form-decidir button.primario").click();
        assertThat(page.locator("#toast")).containsText("PC-2026-");
        assertThat(page.locator("#detalhe")).containsText("Fechada");

        // Recebe 12 de 32 caixas: pedido fica parcialmente recebido
        page.locator("[data-aba=pedidos]").click();
        page.locator("[data-receber]").first().click();
        page.locator("[data-receber-produto]").first().fill("12");
        page.locator("#modal-conteudo button.primario").click();
        assertThat(page.locator("#tb-pedidos")).containsText("Parcialmente recebido");
        assertThat(page.locator("#tb-pedidos")).containsText("12/32");

        // O extrato do produto mostra a entrada vinculada ao pedido
        page.locator("[data-aba=produtos]").click();
        page.locator("[data-extrato][data-sku=PAP-A4-500]").click();
        assertThat(page.locator("#modal-conteudo")).containsText("Recebimento");
        assertThat(page.locator("#modal-conteudo")).containsText("PC-2026-");

        assertThat(errosConsole).as("erros no console do navegador").isEmpty();
    }

    @Test
    void senha_errada_mostra_erro_e_troca_de_idioma_traduz_a_tela() {
        page.locator("#form-login [name=username]").fill("comprador");
        page.locator("#form-login [name=senha]").fill("senha-errada");
        page.locator("#form-login button.primario").click();
        assertThat(page.locator("#toast .msg.erro")).isVisible();
        assertThat(page.locator("#tela-app")).isHidden();

        page.locator("#tela-login [data-lang=en]").click();
        assertThat(page.locator("#form-login button.primario")).hasText("Sign in");
        entrar("admin", "admin123456");
        assertThat(page.locator("[data-aba=usuarios]")).hasText("Users");
        page.locator("[data-aba=usuarios]").click();
        assertThat(page.locator("#tb-usuarios")).containsText("aprovador");

        // O 401 do login errado aparece no console como falha de rede; nenhum outro erro pode existir
        assertThat(errosConsole).allMatch(e -> e.contains("401"), "apenas o 401 esperado do login");
    }

    private void entrar(String usuario, String senha) {
        page.locator("#form-login [name=username]").fill(usuario);
        page.locator("#form-login [name=senha]").fill(senha);
        page.locator("#form-login button.primario").click();
        assertThat(page.locator("#tela-app")).isVisible();
    }

    private void abrirCotacao(String codigo) {
        page.locator("[data-aba=cotacoes]").click();
        page.locator("#tb-cotacoes tr", new Page.LocatorOptions().setHasText(codigo)).click();
        assertThat(page.locator("#detalhe")).containsText(codigo);
    }
}
