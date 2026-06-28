package com.ebanx.api.accountasset.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Cenários complementares executados de forma isolada, sem depender da ordem oficial do Ipkiss Tester.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AccountAssetRobustnessE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void resetState() throws Exception {
        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    @DisplayName("Deve transferir para conta (account) que já possui saldo")
    void deveTransferirParaAccountExistente() throws Exception {
        deposit("100", "30");
        deposit("300", "7");

        mockMvc.perform(event("""
                {"type":"transfer","origin":"100","destination":"300","amount":12}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin.balance").value(18))
                .andExpect(jsonPath("$.destination.balance").value(19));
    }

    @Test
    @DisplayName("Falha de saque (witdraw) não deve alterar o saldo (amount)")
    void withdrawSemSaldoNaoDeveAlterarEstado() throws Exception {
        deposit("100", "10");

        mockMvc.perform(event("""
                {"type":"withdraw","origin":"100","amount":11}
                """))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));

        balance("100", "10");
    }

    @Test
    @DisplayName("Falha de transferência (transfer) não deve criar o destino nem debitar a origem")
    void transferSemSaldoNaoDeveAlterarEstado() throws Exception {
        deposit("100", "10");

        mockMvc.perform(event("""
                {"type":"transfer","origin":"100","destination":"300","amount":11}
                """))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));

        balance("100", "10");
        mockMvc.perform(get("/balance").param("account_id", "300"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("Transferência (transfer) para a própria conta não deve alterar o saldo (amount)")
    void transferParaMesmaAccountNaoDeveAlterarEstado() throws Exception {
        deposit("100", "10");

        mockMvc.perform(event("""
                {"type":"transfer","origin":"100","destination":"100","amount":5}
                """))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));

        balance("100", "10");
    }

    @Test
    @DisplayName("Deve permitir saque (withdraw) do saldo total e reutilizar a conta (account) zerada")
    void deveSacarSaldoTotalEReutilizarAccount() throws Exception {
        deposit("100", "10");

        mockMvc.perform(event("""
                {"type":"withdraw","origin":"100","amount":10}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin.balance").value(0));

        deposit("100", "3");
        balance("100", "3");
    }

    @Test
    @DisplayName("Reset intermediário deve remover todas as contas (accounts)")
    void resetIntermediarioDeveRemoverTodasAccounts() throws Exception {
        deposit("100", "10");
        deposit("200", "20");

        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        for (String accountId : new String[]{"100", "200"}) {
            mockMvc.perform(get("/balance").param("account_id", accountId))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("0"));
        }
    }

    @Test
    @DisplayName("Deve preservar precisão decimal em operações encadeadas")
    void devePreservarPrecisaoDecimal() throws Exception {
        deposit("100", "0.10");
        deposit("100", "0.20");

        mockMvc.perform(event("""
                {"type":"withdraw","origin":"100","amount":0.05}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin.balance").value(0.25));

        balance("100", "0.25");
    }

    @Test
    @DisplayName("Valores zero, negativos e ausentes devem manter 404 com zero")
    void valoresInvalidosDevemManterContrato() throws Exception {
        String[] payloads = {
                "{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":0}",
                "{\"type\":\"deposit\",\"destination\":\"100\",\"amount\":-1}",
                "{\"type\":\"deposit\",\"destination\":\"100\"}",
                "{\"type\":\"withdraw\",\"origin\":\"100\",\"amount\":0}",
                "{\"type\":\"transfer\",\"origin\":\"100\",\"destination\":\"200\",\"amount\":-1}"
        };

        for (String payload : payloads) {
            mockMvc.perform(event(payload))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("0"));
        }
    }

    @Test
    @DisplayName("IDs inválidos devem manter 404 com zero")
    void idsInvalidosDevemManterContrato() throws Exception {
        String[] payloads = {
                "{\"type\":\"deposit\",\"destination\":\"abc\",\"amount\":10}",
                "{\"type\":\"deposit\",\"destination\":\"\",\"amount\":10}",
                "{\"type\":\"deposit\",\"destination\":\"12345678901\",\"amount\":10}",
                "{\"type\":\"withdraw\",\"amount\":10}",
                "{\"type\":\"transfer\",\"origin\":\"100\",\"amount\":10}"
        };

        for (String payload : payloads) {
            mockMvc.perform(event(payload))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("0"));
        }
    }

    @Test
    @DisplayName("Operação inexistente e variação de caixa devem manter 400")
    void tiposDeEventoInvalidosDevemManterBadRequest() throws Exception {
        for (String type : new String[]{"unknown", "DEPOSIT", ""}) {
            mockMvc.perform(event("""
                    {"type":"%s","destination":"100","amount":10}
                    """.formatted(type)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Payload vazio, malformado e null devem resultar em 400")
    void payloadsSintaticamenteInvalidosDevemManterBadRequest() throws Exception {
        String[] payloads = {"", "{", "null"};

        for (String payload : payloads) {
            mockMvc.perform(event(payload))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    @DisplayName("Falhar antes de criar contas não deve impedir operações posteriores")
    void falhaInicialNaoDeveContaminarOperacoesPosteriores() throws Exception {
        mockMvc.perform(event("""
                {"type":"transfer","origin":"999","destination":"300","amount":15}
                """))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));

        deposit("999", "20");

        mockMvc.perform(event("""
                {"type":"transfer","origin":"999","destination":"300","amount":15}
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin.balance").value(5))
                .andExpect(jsonPath("$.destination.balance").value(15));
    }

    private void deposit(String accountId, String amount) throws Exception {
        mockMvc.perform(event("""
                {"type":"deposit","destination":"%s","amount":%s}
                """.formatted(accountId, amount)))
                .andExpect(status().isCreated());
    }

    private void balance(String accountId, String expectedBalance) throws Exception {
        mockMvc.perform(get("/balance").param("account_id", accountId))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedBalance));
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder event(String payload) {
        return post("/event")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload);
    }
}
