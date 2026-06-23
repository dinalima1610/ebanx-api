package com.ebanx.api.accountasset.repository;

import com.ebanx.api.accountasset.domain.AccountAsset;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repositório em memória para o armazenamento e gerenciamento de ativos de contas.
 * Utiliza estruturas thread-safe para garantir a consistência de estado sob alta concorrência,
 * eliminando dependências relacionais seguindo o princípio YAGNI (You Ain't Gonna Need It).
 */
@Repository
public class AccountAssetRepository {
    private final ConcurrentHashMap<String, AccountAsset> storeConcurrentHashMap = new ConcurrentHashMap<>();

    public Optional<AccountAsset> findById(String id) {
        return Optional.ofNullable(storeConcurrentHashMap.get(id));
    }

    public AccountAsset save(AccountAsset accountAsset) {
        storeConcurrentHashMap.put(accountAsset.getAccountId(), accountAsset);
        return accountAsset;
    }

    public boolean existsById(String id) {
        return storeConcurrentHashMap.containsKey(id);
    }

    public void deleteAll() {
        storeConcurrentHashMap.clear();
    }
}
