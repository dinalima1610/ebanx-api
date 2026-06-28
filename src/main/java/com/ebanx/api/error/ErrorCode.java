package com.ebanx.api.error;

/**
 * Códigos para identificar falhas de negócio sem depender do texto da mensagem.
 */
public enum ErrorCode {
    INVALID_ACCOUNT,
    INVALID_AMOUNT,
    ACCOUNT_NOT_FOUND,
    INSUFFICIENT_BALANCE,
    SAME_ACCOUNT_TRANSFER
}
