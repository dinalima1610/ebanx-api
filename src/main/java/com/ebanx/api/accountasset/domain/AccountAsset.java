package com.ebanx.api.accountasset.domain;

import java.math.BigDecimal;
import lombok.Value;

/**
 * Classe de domínio que representa o estado financeiro de uma conta (account).
 * Imutável para impedir alterações de saldo fora da operação coordenada pelo serviço.
 */
@Value
public class AccountAsset {
    String accountId;
    BigDecimal amount;
}
