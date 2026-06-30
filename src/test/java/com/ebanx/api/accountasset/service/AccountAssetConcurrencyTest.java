package com.ebanx.api.accountasset.service;

import com.ebanx.api.accountasset.repository.AccountAssetRepository;
import com.ebanx.api.error.BusinessException;
import com.ebanx.api.error.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
   * Testes de concorrência das operações financeiras utilizando o repositório
   * real em memória e múltiplas threads coordenadas.
   *
   * Valida que depósitos não perdem atualizações, saques não produzem saldo
   * negativo, transferências preservam o total financeiro e operações recusadas
   * não alteram o estado das contas.
   *
   * As garantias verificadas são restritas a uma única instância da aplicação
   * e não representam durabilidade ou atomicidade distribuída.
   */
class AccountAssetConcurrencyTest {

    private AccountAssetService service;

    @BeforeEach
    void setUp() {
        service = new AccountAssetService(
                new AccountAssetRepository(),
                new AccountValidatorService());
    }

    @Test
    @DisplayName("Depósitos concorrentes não devem perder atualizações")
    void concurrentDepositsMustNotLoseUpdates() {
        int operations = 100;

        runConcurrently(operations, () -> service.deposit("100", BigDecimal.ONE));

        assertEquals(0, new BigDecimal("100").compareTo(service.getBalance("100").getAmount()));
    }

    @Test
    @DisplayName("Saques concorrentes não devem produzir saldo negativo")
    void concurrentWithdrawalsMustNotProduceNegativeBalance() {
        int operations = 200;
        AtomicInteger successfulWithdrawals = new AtomicInteger();
        AtomicInteger rejectedWithdrawals = new AtomicInteger();
        service.deposit("100", new BigDecimal("100"));

        runConcurrently(operations, () -> {
            try {
                service.withdraw("100", BigDecimal.ONE);
                successfulWithdrawals.incrementAndGet();
            } catch (BusinessException exception) {
                assertEquals(ErrorCode.INSUFFICIENT_BALANCE, exception.getCode());
                rejectedWithdrawals.incrementAndGet();
            }
        });

        assertEquals(100, successfulWithdrawals.get());
        assertEquals(100, rejectedWithdrawals.get());
        assertEquals(0, BigDecimal.ZERO.compareTo(service.getBalance("100").getAmount()));
    }

    @Test
    @DisplayName("Transferências concorrentes devem preservar o total financeiro")
    void concurrentTransfersMustPreserveTotalBalance() {
        int operations = 200;
        service.deposit("100", new BigDecimal("1000"));
        service.deposit("200", new BigDecimal("1000"));
        AtomicInteger sequence = new AtomicInteger();

        runConcurrently(operations, () -> {
            if (sequence.getAndIncrement() % 2 == 0) {
                service.transfer("100", "200", BigDecimal.ONE);
            } else {
                service.transfer("200", "100", BigDecimal.ONE);
            }
        });

        BigDecimal total = service.getBalance("100").getAmount()
                .add(service.getBalance("200").getAmount());

        assertEquals(0, new BigDecimal("2000").compareTo(total));
        assertEquals(0, new BigDecimal("1000").compareTo(service.getBalance("100").getAmount()));
        assertEquals(0, new BigDecimal("1000").compareTo(service.getBalance("200").getAmount()));
    }

    @Test
    @DisplayName("Transferência recusada deve preservar as duas contas")
    void rejectedTransferMustPreserveBothAccounts() {
        service.deposit("100", new BigDecimal("10"));
        service.deposit("200", new BigDecimal("20"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> service.transfer("100", "200", new BigDecimal("11")));

        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, exception.getCode());
        assertEquals(0, new BigDecimal("10").compareTo(service.getBalance("100").getAmount()));
        assertEquals(0, new BigDecimal("20").compareTo(service.getBalance("200").getAmount()));
    }

    private void runConcurrently(int operations, Runnable operation) {
        ExecutorService executor = Executors.newFixedThreadPool(operations);
        CountDownLatch ready = new CountDownLatch(operations);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(operations);
        Queue<Throwable> failures = new ConcurrentLinkedQueue<>();

        for (int index = 0; index < operations; index++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    operation.run();
                } catch (Throwable failure) {
                    failures.add(failure);
                } finally {
                    done.countDown();
                }
            });
        }

        try {
            assertTrue(ready.await(5, TimeUnit.SECONDS), "As operações não ficaram prontas a tempo");
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS), "Possível deadlock nas operações concorrentes");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Teste concorrente interrompido", exception);
        } finally {
            executor.shutdownNow();
        }

        assertTrue(
                failures.isEmpty(),
                () -> "Falhas concorrentes: " + failures);
    }
}
