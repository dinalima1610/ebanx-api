package com.ebanx.api.accountasset.repository;

import com.ebanx.api.accountasset.domain.AccountAsset;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    //para eventuais testes
    @jakarta.annotation.PostConstruct
    public void initData() {
        this.save(new AccountAsset("987654321X", new BigDecimal("250.00")));
        this.save(new AccountAsset("1234567890", BigDecimal.ZERO));
    }
}
