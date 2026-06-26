package com.ebanx.api.accountasset.service;

import com.ebanx.api.accountasset.domain.AccountAsset;
import com.ebanx.api.accountasset.repository.AccountAssetRepository;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

/**
 * Classe de serviço responsável por centralizar as regras de negócio financeiras.
 * Concentra os fluxos lógicos e matemáticos de depósitos (deposit), saques (withdraw) e transferências (transfer),
 * mantendo-se totalmente desacoplada e isolada de protocolos de transporte HTTP.
 */
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
                .orElseThrow(() -> new IllegalArgumentException(AccountMessages.CONTA_NAO_ENCONTRADA_OU_SEM_SALDO_INICIAL));
    }

    public AccountAsset deposit(String accountId, BigDecimal amount) {
        if (!accountValidator.isValidAccountId(accountId)) {
            throw new IllegalArgumentException(AccountMessages.CONTA_INVALIDA);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(AccountMessages.VALOR_DEPOSITO_POSITIVO);
        }

        //se não achar account (conta), instancia uma nova
        AccountAsset accountAsset = accountAssetRepository.findById(accountId)
                .orElseGet(() -> new AccountAsset(accountId, BigDecimal.ZERO));

        //adiciona ammount (saldo)
        accountAsset.setAmount(accountAsset.getAmount().add(amount));

        return accountAssetRepository.save(accountAsset);
    }

    public AccountAsset withdraw(String accountId, BigDecimal amount) {
        //verifica se é conta (account) válida
        if (!accountValidator.isValidAccountId(accountId)) {
            throw new IllegalArgumentException(AccountMessages.CONTA_INVALIDA);
        }

        //verifica se o valor (amount) do saque foi informado e é positivo
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(AccountMessages.VALOR_SAQUE_POSITIVO);
        }

        //lança erro se a account (conta) não existir no repositório
        AccountAsset accountAsset = accountAssetRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException(AccountMessages.CONTA_NAO_ENCONTRADA));

        //impede que o saque (withdraw) maior que saldo (amount)
        if (accountAsset.getAmount().subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException(AccountMessages.SALDO_INSUFICIENTE);
        }

        //subtrai amount (saldo)
        accountAsset.setAmount(accountAsset.getAmount().subtract(amount));

        return accountAssetRepository.save(accountAsset);
    }

    public List<AccountAsset> transfer(String origId, String destId, BigDecimal amount) {
        //valida os ids antes de qualquer mutação para evitar débito parcial na transferência
        if (!accountValidator.isValidAccountId(origId) || !accountValidator.isValidAccountId(destId)) {
            throw new IllegalArgumentException(AccountMessages.CONTA_NAO_ENCONTRADA);
        }

        //account (conta) origem e destino devem ser diferenes
        if (origId.equals(destId)) {
            throw new IllegalArgumentException(AccountMessages.ORIGEM_IGUAL_DESTINO);
        }

        //o método withdraw valida amount (saldo) insuficiente e valor positivo para a origem
        AccountAsset origAccount = this.withdraw(origId, amount);

        //o método deposit trata a criação dinâmica da account (conta) de destino caso ela não exista
        AccountAsset destAccount = this.deposit(destId, amount);

        return List.of(origAccount, destAccount);
    }
}
