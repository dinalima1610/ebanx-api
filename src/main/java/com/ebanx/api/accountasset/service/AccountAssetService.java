package com.ebanx.api.accountasset.service;

import com.ebanx.api.accountasset.domain.AccountAsset;
import com.ebanx.api.accountasset.repository.AccountAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Service
public class AccountAssetService {

    private final AccountAssetRepository accountAssetRepository;
    private final AccountValidatorService accountValidator;

    public AccountAssetService(AccountAssetRepository accountAssetRepository, AccountValidatorService accountValidator) {
        this.accountAssetRepository = accountAssetRepository;
        this.accountValidator       = accountValidator;
    }

    @Transactional(readOnly = true)
    public AccountAsset getBalance(String accountId) {
        return accountAssetRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada ou sem saldo inicial"));
    }

    @Transactional
    public AccountAsset deposit(String accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo");
        }

        //valida se a conta existe e é válida
        AccountAsset accountAsset = accountAssetRepository.findById(accountId)
                .orElseThrow(() -> {
                    if (!accountValidator.accountExists(accountId)) {
                        return new IllegalArgumentException("Conta inválida ou inexistente");
                    }
                    return new IllegalArgumentException("Conta não encontrada");
                });

        //adiciona o saldo
        accountAsset.setAmount(accountAsset.getAmount().add(amount));

        return accountAssetRepository.save(accountAsset);
    }

    @Transactional
    public AccountAsset withdraw(String accountId, BigDecimal amount) {
        //lança erro se o saldo for insuficiente
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser positivo");
        }

        //idem se não existir a conta ou for inválida
        AccountAsset accountAsset = accountAssetRepository.findById(accountId)
                .orElseThrow(() -> {
                    if (!accountValidator.accountExists(accountId)) {
                        return new IllegalArgumentException("Conta de origem inválida ou inexistente");
                    }
                    return new IllegalArgumentException("Conta não encontrada");
                });

        //impede saldo menor que zero
        if (accountAsset.getAmount().subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }

        //subtrai o saldo
        accountAsset.setAmount(accountAsset.getAmount().subtract(amount));

        return accountAssetRepository.save(accountAsset);
    }

    @Transactional
    public AccountAsset transfer(String origId, String destId, BigDecimal amount) {
        //a conta origem e destino devem ser diferenes
        if (origId.equals(destId)) {
            throw new IllegalArgumentException("A conta de origem não pode ser igual à de destino");
        }

        //saque na conta de origem: se não existir a conta origem ou for inválida
        AccountAsset origAccount = accountAssetRepository.findById(origId)
                .orElseThrow(() -> {
                    if (!accountValidator.accountExists(origId)) {
                        return new IllegalArgumentException("Conta de origem inválida ou inexistente");
                    }
                    return new IllegalArgumentException("Conta de origem não encontrada");
                });

        //lança erro se o valor for insuficiente
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do saque deve ser positivo");
        }

        //impede saldo menor que zero
        if (origAccount.getAmount().subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }
        origAccount.setAmount(origAccount.getAmount().subtract(amount));
        accountAssetRepository.save(origAccount);

        //depósito na conta de destino: se não existir a conta destino ou for inválida
        AccountAsset destAccount = accountAssetRepository.findById(destId)
                .orElseThrow(() -> {
                    if (!accountValidator.accountExists(destId)) {
                        return new IllegalArgumentException("Conta de destino inválida ou inexistente");
                    }
                    return new IllegalArgumentException("Conta de destino não encontrada");
                });

        destAccount.setAmount(destAccount.getAmount().add(amount));
        accountAssetRepository.save(destAccount);

        return origAccount;
    }
}