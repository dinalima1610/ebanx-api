package com.ebanx.api.accountasset.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes Unitários de Domínio para a entidade AccountAsset.
 * Valida de forma isolada e veloz a consistência das propriedades básicas do modelo,
 * a integridade dos seus construtores e o comportamento esperado de mutação de estado via métodos getters e setters.
 */
public class AccountAssetTest {
    @Test
    @DisplayName("Deve instanciar um AccountAsset com os valores (accountId e amount) corretos através do construtor")
    void deveInstanciarComValoresCorretos() {
        //configuração do cenário (arrange)
        String idExpec = "200";
        BigDecimal amountExpec = new BigDecimal("10");

        //execução da ação (act)
        AccountAsset asset = new AccountAsset(idExpec, amountExpec);

        //validação do resultado (assert)
        assertNotNull(asset);
        assertEquals(idExpec, asset.getAccountId());
        assertEquals(amountExpec, asset.getAmount());
        assertEquals(amountExpec, asset.getAmount());
    }

    @Test
    @DisplayName("Deve permitir alterar o saldo (amount) através do método setter e recuperar o novo saldo pelo método getter")
    void deveAlterarAmountComSetter() {
        AccountAsset accountAsset = new AccountAsset("200", BigDecimal.ZERO);
        BigDecimal newAmount = new BigDecimal("5");

        accountAsset.setAmount(newAmount);

        assertEquals(newAmount, accountAsset.getAmount());
    }
}
