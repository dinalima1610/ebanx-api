package com.ebanx.api.accountasset.controller;

import com.ebanx.api.accountasset.service.AccountAssetService;
import com.ebanx.api.accountasset.domain.AccountAsset;
import com.ebanx.api.accountasset.controller.dto.AccountAssetRecord;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("api")
public class AccountAssetController {
    private final AccountAssetService accountAssetService;

    public AccountAssetController(AccountAssetService accountAssetService) {
        this.accountAssetService = accountAssetService;
    }

    @GetMapping("v1/balance/{accountId}")
    public ResponseEntity<AccountAsset> getBalance (
            @PathVariable String accountId) {

        try {
            AccountAsset accountAsset = accountAssetService.getBalance(accountId);
            return ResponseEntity.ok(accountAsset);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("v1/event/{entryType}/{accountId}")
    public ResponseEntity<AccountAsset> event (
            @PathVariable String accountId,
            @PathVariable String entryType,
            @Valid @RequestBody AccountAssetRecord request) {

        if (entryType.length() > 1 || (!"D".equalsIgnoreCase(entryType) && !"W".equalsIgnoreCase(entryType) && !"T".equalsIgnoreCase(entryType))) {
            return ResponseEntity.badRequest().build(); //rRetorna HTTP 400
        }

        boolean isTransfer = "T".equalsIgnoreCase(entryType);
        //se for transferência, a conta de destino é obrigatória
        if (isTransfer && (request.accountDestId() == null || request.accountDestId().equals(""))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("Reason", "Destination account is required for transfers")
                    .build();
        }

        AccountAsset accountAssetResult = switch (entryType.toUpperCase()) {
            case "D" ->
                    accountAssetService.deposit(accountId, request.amount());
            case "W" ->
                    accountAssetService.withdraw(accountId, request.amount());
            case "T" ->
                    accountAssetService.transfer(accountId, request.accountDestId(), request.amount());
            default ->
                    throw new IllegalArgumentException("Operação não suportada");
        };

        return ResponseEntity.ok(accountAssetResult);
    }
}
