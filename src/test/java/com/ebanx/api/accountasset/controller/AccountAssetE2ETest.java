package com.ebanx.api.accountasset.controller;

import com.ebanx.api.accountasset.repository.AccountAssetRepository;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AccountAssetE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AccountAssetRepository accountAssetRepository;

    @Test
    @DisplayName("Deve fazer reset de todos os valores antes de iniciar os testes")
    @Order(1)
    void deveFazerResetStateAntesDeIniciarTestes() throws Exception {
        mockMvc.perform(post("/reset"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    @DisplayName("Obter o saldo (balance) para uma conta (account) não existente")
    @Order(2)
    void obterBalanceParaAccountNaoExistente() throws Exception {
        mockMvc.perform(get("/balance").param("account_id", "1234"))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("Deve criar uma conta (account) com um saldo (balance) inicial")
    @Order(3)
    void deveCriarAccountComBalanceInicial() throws Exception {
        String payload = "{\"type\":\"deposit\", \"destination\":\"100\", \"amount\":10}";

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destination.id").value("100"))
                .andExpect(jsonPath("$.destination.balance").value(10));
    }

    @Test
    @DisplayName("Deve depositar (deposit) em uma conta (account) existente")
    @Order(4)
    void deveFazerDepositAccountExistente() throws Exception {
        String payload = "{\"type\":\"deposit\", \"destination\":\"100\", \"amount\":10}";

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.destination.id").value("100"))
                .andExpect(jsonPath("$.destination.balance").value(20));
    }

    @Test
    @DisplayName("Deve obter o saldo (balance) para uma conta (account) existente")
    @Order(5)
    void deveObterBalanceParaAccountExistente() throws Exception {
        mockMvc.perform(get("/balance").param("account_id", "100"))
                .andExpect(status().isOk())
                .andExpect(content().string("20"));
    }

    @Test
    @DisplayName("Fazer saque (withdraw) para uma conta (account) não existente")
    @Order(6)
    void fazerWithdrawParaAccountNaoExistente() throws Exception {
        String payload = "{\"type\":\"withdraw\", \"origin\":\"200\", \"amount\":10}";

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("Deve fazer saque (withdraw) para de uma conta (account) existente")
    @Order(7)
    void deveFazerWithdrawParaAccountExistente() throws Exception {
        String payload = "{\"type\":\"withdraw\", \"origin\":\"100\", \"amount\":5}";

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin.id").value("100"))
                .andExpect(jsonPath("$.origin.balance").value(15));
    }

    @Test
    @DisplayName("Deve transferir (transfer) de uma conta (account) existente")
    @Order(8)
    void deveRransferParaAccountExistente() throws Exception {
        String payload = "{\"type\":\"transfer\", \"origin\":\"100\", \"amount\":15, \"destination\":\"300\"}";

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin.id").value("100"))
                .andExpect(jsonPath("$.origin.balance").value(0))
                .andExpect(jsonPath("$.destination.id").value("300"))
                .andExpect(jsonPath("$.destination.balance").value(15));
    }

    @Test
    @DisplayName("Fazer transferência (transfer) de uma conta (account) não existente")
    @Order(9)
    void fazerTransferDeAccountNaoExistente() throws Exception {
        String payload = "{\"type\":\"transfer\", \"origin\":\"200\", \"amount\":15, \"destination\":\"300\"}";

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }

    @Test
    @DisplayName("Fazer transferência (transfer) para uma conta (account) não existente")
    @Order(10)
    void fazerTransferParaAccountNaoExistente() throws Exception {
        String payload = "{\"type\":\"transfer\", \"origin\":\"200\", \"amount\":15, \"destination\":\"400\"}";

        mockMvc.perform(post("/event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound())
                .andExpect(content().string("0"));
    }
}
