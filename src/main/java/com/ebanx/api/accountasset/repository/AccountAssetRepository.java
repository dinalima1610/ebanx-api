package com.ebanx.api.accountasset.repository;

import com.ebanx.api.accountasset.domain.AccountAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountAssetRepository extends JpaRepository<AccountAsset, String> {
}
