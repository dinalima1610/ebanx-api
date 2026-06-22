package com.ebanx.api.accountasset.service;

import org.springframework.stereotype.Component;
@Component
public class AccountValidatorService {

    public boolean accountExists(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            return false;
        }

        //garante que a conta tenha entre 1 e 10 caracteres e termina com números ou "X"/"x" (Banco do Brasil tem contas terminas em X)
        return accountId.matches("^[0-9A-Xa-x]{1,10}$");
    }
}
