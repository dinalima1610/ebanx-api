package com.ebanx.api.accountasset.controller;

import com.ebanx.api.accountasset.service.AccountAssetService;
import com.ebanx.api.accountasset.domain.AccountAsset;
import com.ebanx.api.accountasset.controller.dto.AccountAssetRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import org.springframework.http.ResponseEntity;

import java.util.Map;

/**
 * Controlador REST responsável por expor as portas de entrada HTTP da API na raiz do servidor.
 * Recebe requisições, delega os fluxos financeiros para a camada de serviço
 * e traduz resultados e exceções para respostas HTTP em conformidade com o Ipkiss Tester.
 */
@RestController
public class AccountAssetController {
    @Autowired
    private AccountAssetService accountAssetService;

    @PostMapping("/reset")
    public ResponseEntity<String> reset() {
        accountAssetService.reset();
        return ResponseEntity.ok("OK");
    }

    @GetMapping("/balance")
    public ResponseEntity<Object> getBalance (@RequestParam("account_id") String accountId) {
        AccountAsset accountAsset = accountAssetService.getBalance(accountId);
        return ResponseEntity.ok(accountAsset.getAmount());
    }

    @PostMapping("/event")
    public ResponseEntity<Object> handleEvent (@RequestBody AccountAssetRecord requestAccountAssetRecord) {
        //deposit
        if ("deposit".equals(requestAccountAssetRecord.type())) {
            AccountAsset accountAsset = accountAssetService.deposit(requestAccountAssetRecord.destination(), requestAccountAssetRecord.amount());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("destination", Map.of("id", accountAsset.getAccountId(), "balance", accountAsset.getAmount())));
        }

        //withdraw
        if ("withdraw".equals(requestAccountAssetRecord.type())) {
            AccountAsset accountAsset = accountAssetService.withdraw(requestAccountAssetRecord.origin(), requestAccountAssetRecord.amount());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("origin", Map.of("id", accountAsset.getAccountId(), "balance", accountAsset.getAmount())));
        }

        //transfer
        if ("transfer".equals(requestAccountAssetRecord.type())) {
            var result = accountAssetService.transfer(requestAccountAssetRecord.origin(), requestAccountAssetRecord.destination(), requestAccountAssetRecord.amount());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "origin", Map.of("id", result.get(0).getAccountId(), "balance", result.get(0).getAmount()),
                            "destination", Map.of("id", result.get(1).getAccountId(), "balance", result.get(1).getAmount())
                    ));
        }

        return ResponseEntity.badRequest().build();
    }
}
