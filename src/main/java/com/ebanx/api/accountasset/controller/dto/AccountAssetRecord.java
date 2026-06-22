package com.ebanx.api.accountasset.controller.dto;

import java.math.BigDecimal;
public record AccountAssetRecord (
        //"deposit", "withdraw" ou "transfer"
        String type,
        //id da conta de origem
        String origin,
        //id da conta de destino
        String destination,
        //valor da operação
        BigDecimal amount
) {
}