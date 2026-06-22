package com.ebanx.api.accountasset.service;

import org.springframework.stereotype.Component;
@Component
public class AccountValidatorService {

    public boolean accountExists(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }

         //trava o tamanho de 1 a 10 dígitos numéricos puros, mantendo o código flexível
        return accountId.matches("^[0-9]{1,10}$");
    }
}
