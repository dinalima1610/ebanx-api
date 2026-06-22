package com.ebanx.api.accountasset.service;

import com.ebanx.api.accountasset.domain.AccountAsset;
import com.ebanx.api.accountasset.repository.AccountAssetRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountAssetService {

    private final AccountAssetRepository accountAssetRepository;
    private final AccountValidatorService accountValidator;

    public AccountAssetService(AccountAssetRepository accountAssetRepository, AccountValidatorService accountValidator) {
        this.accountAssetRepository = accountAssetRepository;
        this.accountValidator       = accountValidator;
    }

    public AccountAsset getBalance(String accountId) {
        return accountAssetRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Conta não encontrada ou sem saldo inicial"));
    }

    public AccountAsset deposit(String accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor do depósito deve ser positivo");
        }

        //se não achar conta, instancia uma nova
        AccountAsset accountAsset = accountAssetRepository.findById(accountId)
                .orElseGet(() -> new AccountAsset(accountId, BigDecimal.ZERO));

        //adiciona o saldo
        accountAsset.setAmount(accountAsset.getAmount().add(amount));

        return accountAssetRepository.save(accountAsset);
    }

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

    public List<AccountAsset> transfer(String origId, String destId, BigDecimal amount) {
        //a conta origem e destino devem ser diferenes
        if (origId.equals(destId)) {
            throw new IllegalArgumentException("A conta de origem não pode ser igual à de destino");
        }

        //o método withdraw valida saldo insuficiente e valor positivo para a origem
        AccountAsset origAccount = this.withdraw(origId, amount);

        //o método deposit trata a criação dinâmica da conta de destino caso ela não exista
        AccountAsset destAccount = this.deposit(destId, amount);

        return List.of(origAccount, destAccount);
    }
}