package com.ebanx.api.accountasset.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class AccountAssetTest {
    @Test
    @DisplayName("Deve instanciar um AccountAsset com os valores corretos através do construtor")
    void deveInstanciarComValoresCorretos() {
        //configuração do cenário (arrange)
        String idEsperado = "1234567890";
        BigDecimal amountEsperado = new BigDecimal("1500.50");

        //execução da ação (act)
        AccountAsset asset = new AccountAsset(idEsperado, amountEsperado);

        //validação do resultado (assert)
        assertNotNull(asset);
        assertEquals(idEsperado, asset.getAccountId());
        assertEquals(amountEsperado, asset.getAmount());
        //versão inicial deve ser zero
        assertEquals(0, asset.getVersion());
    }

    @Test
    @DisplayName("Deve permitir alterar o saldo (amount) através do método setter e recuperar o novo saldo pelo método getter")
    void deveAlterarAmountComSetter() {
        AccountAsset accountAsset = new AccountAsset("1234567890", BigDecimal.ZERO);
        BigDecimal newAmount = new BigDecimal("250.00");

        accountAsset.setAmount(newAmount);

        assertEquals(newAmount, accountAsset.getAmount());
    }
}
