package com.ebanx.api.accountasset.domain;

import java.math.BigDecimal;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Classe de domínio que representa o estado financeiro de uma conta.
 * Gerenciada em memória de forma simplificada atendendo aos critérios da especificação.
 */
@Data
//para o Lombok gerar getters e setters, hashCode e equals
@NoArgsConstructor
public class AccountAsset {
    private String accountId;
    private BigDecimal amount = BigDecimal.ZERO;

    public AccountAsset(String accountId, BigDecimal amount) {
        this.accountId = accountId;
        this.amount = amount;
    }
}