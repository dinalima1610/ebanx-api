package com.ebanx.api.accountasset.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data //para o Lombok gerar getters e setters, hashCode e equals
@NoArgsConstructor
@AllArgsConstructor
@Entity(name = "AccountAsset")
@Table(name = "account_asset", schema = "ebanx_api")
public class AccountAsset {

    @Id
    @Column(name = "account_id", length = 15)
    private String accountId;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Version //proteção contra concorrência via bloqueio otimista
    @Column(name = "version")
    private int version;

    public AccountAsset(String accountId, BigDecimal amount) {
        this.accountId = accountId;
        this.amount = amount;
    }
}