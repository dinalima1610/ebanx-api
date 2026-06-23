package com.ebanx.api.accountasset.controller.dto;

import java.math.BigDecimal;
/**
 * Objeto responsável pela transferência de dados (DTO) imutável que representa o payload recebido nas operações financeiras.
 * Centraliza o mapeamento sintático das requisições unificadas de eventos da API bancária.
 */
public record AccountAssetRecord (
        //"deposit", "withdraw" ou "transfer"
        String type,
        //id account (conta) de origem
        String origin,
        //id account (conta) de destino
        String destination,
        //amount (valor) da operação
        BigDecimal amount
) {
}