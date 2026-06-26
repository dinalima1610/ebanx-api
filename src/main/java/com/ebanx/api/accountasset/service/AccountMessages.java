package com.ebanx.api.accountasset.service;

/**
 * Classe utilitária para a centralização de constantes textuais de mensagens de erro.
 * Segue o princípio DRY (Don't Repeat Yourself), mitigando o acoplamento de strings
 * e unificando as validações entre o código de produção e a suíte de testes.
 */
public final class AccountMessages {
    private AccountMessages() {
        //private para impedir instanciação e manter o padrão utilitário
    }

    public static final String VALOR_DEPOSITO_POSITIVO = "O valor do depósito deve ser positivo";
    public static final String VALOR_SAQUE_POSITIVO    = "O valor do saque deve ser positivo";
    public static final String CONTA_NAO_ENCONTRADA    = "Conta não encontrada";
    public static final String CONTA_INVALIDA          = "Conta inválida";
    public static final String ORIGEM_IGUAL_DESTINO    = "A conta de origem não pode ser igual à de destino";
    public static final String CONTA_NAO_ENCONTRADA_OU_SEM_SALDO_INICIAL = "Conta não encontrada ou sem saldo inicial";
    public static final String SALDO_INSUFICIENTE      = "Saldo insuficiente";
    public static final String SALDO_FINAL_APOS_SAQUE  = "O saldo final da conta deve ser o valor exato do saldo inicial subtraído do saque";
}




