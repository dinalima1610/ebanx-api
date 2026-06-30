package com.ebanx.api.accountasset.service;

import com.ebanx.api.accountasset.domain.AccountAsset;
import com.ebanx.api.accountasset.repository.AccountAssetRepository;
import com.ebanx.api.error.BusinessException;
import com.ebanx.api.error.ErrorCode;
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

    public synchronized AccountAsset getBalance(String accountId) {
        return accountAssetRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        AccountMessages.CONTA_NAO_ENCONTRADA_OU_SEM_SALDO_INICIAL));
    }

    public synchronized void reset() {
        accountAssetRepository.deleteAll();
    }

    public synchronized AccountAsset deposit(String accountId, BigDecimal amount) {
        if (!accountValidator.isValidAccountId(accountId)) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT, AccountMessages.CONTA_INVALIDA);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, AccountMessages.VALOR_DEPOSITO_POSITIVO);
        }

        //se não achar account (conta), instancia uma nova
        BigDecimal currentAmount = accountAssetRepository.findById(accountId)
                .map(AccountAsset::getAmount)
                .orElse(BigDecimal.ZERO);

        //adiciona ammount (saldo)
        AccountAsset updatedAccount = new AccountAsset(accountId, currentAmount.add(amount));
        return accountAssetRepository.save(updatedAccount);
    }

    public synchronized AccountAsset withdraw(String accountId, BigDecimal amount) {
        //verifica se é conta (account) válida
        if (!accountValidator.isValidAccountId(accountId)) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT, AccountMessages.CONTA_INVALIDA);
        }

        //verifica se o valor (amount) do saque foi informado e é positivo
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, AccountMessages.VALOR_SAQUE_POSITIVO);
        }

        //lança erro se a account (conta) não existir no repositório
        AccountAsset accountAsset = accountAssetRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        AccountMessages.CONTA_NAO_ENCONTRADA));

        //impede que o saque (withdraw) maior que saldo (amount)
        if (accountAsset.getAmount().subtract(amount).compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, AccountMessages.SALDO_INSUFICIENTE);
        }

        AccountAsset updatedAccount = new AccountAsset(
                accountId,
                //subtrai amount (saldo)
                accountAsset.getAmount().subtract(amount));

        return accountAssetRepository.save(updatedAccount);
    }

    public synchronized List<AccountAsset> transfer(String origId, String destId, BigDecimal amount) {
        //valida toda a operação antes de qualquer mutação
        if (!accountValidator.isValidAccountId(origId) || !accountValidator.isValidAccountId(destId)) {
            throw new BusinessException(ErrorCode.INVALID_ACCOUNT, AccountMessages.CONTA_NAO_ENCONTRADA);
        }

        //account (conta) origem e destino devem ser diferenes
        if (origId.equals(destId)) {
            throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER, AccountMessages.ORIGEM_IGUAL_DESTINO);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT, AccountMessages.VALOR_SAQUE_POSITIVO);
        }

        //acount origem
        AccountAsset origAccount = accountAssetRepository.findById(origId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ACCOUNT_NOT_FOUND,
                        AccountMessages.CONTA_NAO_ENCONTRADA));

        if (origAccount.getAmount().compareTo(amount) < 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE, AccountMessages.SALDO_INSUFICIENTE);
        }

        //acount destino
        BigDecimal destinationAmount = accountAssetRepository.findById(destId)
                .map(AccountAsset::getAmount)
                .orElse(BigDecimal.ZERO);
        
        AccountAsset updatedOrigin = new AccountAsset(
                origId,
                origAccount.getAmount().subtract(amount));
        AccountAsset updatedDestination = new AccountAsset(
                destId,
                destinationAmount.add(amount));

        AccountAsset savedOrigin = accountAssetRepository.save(updatedOrigin);
        AccountAsset savedDestination = accountAssetRepository.save(updatedDestination);

        return List.of(savedOrigin, savedDestination);
    }
}
