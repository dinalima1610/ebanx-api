package com.ebanx.api.accountasset.service;

import org.springframework.stereotype.Component;

/**
 * Componente utilitário responsável pela validação sintática dos identificadores de contas.
 * Oferece uma checagem centralizada de presença e formato, sem consultar o repositório.
 */
@Component
public class AccountValidatorService {

    public boolean isValidAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }

        //trava o tamanho de 1 a 10 dígitos numéricos puros, mantendo o código flexível
        return accountId.matches("^[0-9]{1,10}$");
    }
}
