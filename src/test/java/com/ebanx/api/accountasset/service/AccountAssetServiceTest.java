package com.ebanx.api.accountasset.service;

import com.ebanx.api.accountasset.domain.AccountAsset;
import com.ebanx.api.accountasset.repository.AccountAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class AccountAssetServiceTest {

    @Mock
    private AccountAssetRepository accountAssetRepository;
    @Mock
    private AccountValidatorService accountValidatorService;

    @InjectMocks
    private AccountAssetService accountAssetService;

    private final String accountOrigId = "111111111X";
    private final String accountDestId = "3332221110";
    private AccountAsset accountOrig;
    private AccountAsset accountDest;
    private final String accountIdValido      = "987654321X";
    private final String accountIdInvalido    = "XXX654XXX1";
    private final String accountIdInexistente = "9876543210";

    @BeforeEach
    void setUp() {
        //antes de cada teste, reatribui os valores
        accountOrig = new AccountAsset(accountOrigId, new BigDecimal("300.00"), 0);
        accountDest = new AccountAsset(accountDestId, new BigDecimal("100.00"), 0);

        //configura o comportamento estrito do validador para os cenários de id
        Mockito.lenient().when(accountValidatorService.accountExists(accountIdValido)).thenReturn(true);
        Mockito.lenient().when(accountValidatorService.accountExists(accountIdInvalido)).thenReturn(false);
        Mockito.lenient().when(accountValidatorService.accountExists(accountIdInexistente)).thenReturn(false);
    }

    @Nested
    @DisplayName("Teste do fluxo de deposito (deposit)")
    class deposit {

        @Test
        void deveLancarExcecaoQuandoAccountIdForInvalidoPeloValidador() {
            // Arrange
            BigDecimal amount = new BigDecimal("100.00");

            //sSimulando que o banco de dados não encontra o registro (retorna vazio) para que a execução caia no bloco do orElseThrow
            Mockito.when(accountAssetRepository.findById(accountIdInvalido)).thenReturn(java.util.Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                accountAssetService.deposit(accountIdInvalido, amount);
            });

            assertEquals("Conta inválida ou inexistente", exception.getMessage());
        }

        @Test
        @DisplayName("Deve lançar exceção o valor (amount) for negativo")
        void deveLancarExcecaoQuandoAmountForNegativo() {
            BigDecimal amount = new BigDecimal("-50.00");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                accountAssetService.deposit(accountOrigId, amount);
            });

            assertEquals("O valor do depósito deve ser positivo", exception.getMessage());
        }

        @Test
        @DisplayName("Deve lançar exceção o valor (amount) for zero")
        void deveLancarExcecaoQuandoAmountForZero() {
            BigDecimal amount = BigDecimal.ZERO;

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                accountAssetService.deposit(accountOrigId, amount);
            });

            assertEquals("O valor do depósito deve ser positivo", exception.getMessage());
        }

        @Test
        @DisplayName("Não deve permitir valor (amount) zero ou negativo e deve lançar exceção")
        void naoDevePermitirDepositoComValorZeroOuNegativo() {
            BigDecimal amount = new BigDecimal("-10.00");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                accountAssetService.deposit(accountOrigId, amount);
            });

            assertEquals("O valor do depósito deve ser positivo", exception.getMessage());
        }

        @Test
        @DisplayName("Não deve permitir depósito (deposit) se a conta for inválida ou inexistente no validador")
        void naoDevePermitirDepositSeContaForInvalidaOuInexistenteNoValidador() {
            BigDecimal amount = new BigDecimal("10.00");
            Mockito.when(accountAssetRepository.findById(accountIdInexistente)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                accountAssetService.deposit(accountIdInexistente, amount);
            });

            assertEquals("Conta inválida ou inexistente", exception.getMessage());
        }

        @Test
        @DisplayName("Deve depositar (deposit) com sucesso")
        void deveDepositarComSucesso() {
            BigDecimal amount = new BigDecimal("50.00");
            when(accountAssetRepository.findById((accountOrigId))).thenReturn(Optional.of(accountOrig));
            when(accountAssetRepository.save(any(AccountAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AccountAsset accountAssetResult = accountAssetService.deposit(accountOrigId, amount);

            assertNotNull(accountAssetResult);
            //300 + 50 = 350
            assertEquals(new BigDecimal("350.00"), accountAssetResult.getAmount());
            //garante a persistência
            verify(accountAssetRepository, times(1)).save(accountOrig);
        }
    }

    @Nested
    @DisplayName("Teste do fluxo de saque (withdraw)")
    class withdraw {
        @Test
        @DisplayName("Deve reduzir o saldo (amount) e salvar quando houver recursos suficientes")
        void deveSacarComSucesso() {
            BigDecimal amount = new BigDecimal("50.00");
            when(accountAssetRepository.findById(accountOrigId)).thenReturn(Optional.of(accountOrig));
            when(accountAssetRepository.save(any(AccountAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AccountAsset accountAssetResult = accountAssetService.withdraw(accountOrigId, amount);

            assertNotNull(accountAssetResult);
            //300 - 50 = 250
            assertEquals(new BigDecimal("250.00"), accountAssetResult.getAmount());
            verify(accountAssetRepository, times(1)).save(accountOrig);
        }

        @Test
        @DisplayName("Deve lançar exceção e NUNCA pode salvar se o saldo (amount) for menor que o saque solicitado")
        void deveBarrarAmountInsuficiente() {
            //account tem apenas 300
            BigDecimal amount = new BigDecimal("600.00");
            when(accountAssetRepository.findById(accountOrigId)).thenReturn(Optional.of(accountOrig));

            //valida o saldo e lança a exceção
            IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> {
                accountAssetService.withdraw(accountOrigId, amount);
            });

            assertEquals("Saldo insuficiente", illegalStateException.getMessage());
            //garante que nada foi alterado/salvo
            verify(accountAssetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve diminuir o saldo (amount) da conta (account) e persistir")
        void deveDiminuirAmountDaAccountEPersistirNoRepository() {
            BigDecimal amountInit  = new BigDecimal("150.00");
            BigDecimal amount      = new BigDecimal("100.00");
            BigDecimal amountExpec = new BigDecimal("50.00");

            //verificando a mutação de estado, instanciando um objeto de domínio real
            AccountAsset accountAsset = new AccountAsset(accountOrigId, amountInit);
            Mockito.when(accountAssetRepository.findById(accountOrigId)).thenReturn(Optional.of(accountAsset));

            //testando a regra de negócio
            accountAssetService.withdraw(accountOrigId, amount);

            //garantindo que a regra de negócio alterou o saldo do objeto real consistentemente
            assertEquals(0, amountExpec.compareTo(accountAsset.getAmount()),
                    "O saldo final da conta deve ser o valor exato do saldo inicial subtraído do saque");

            //garantindo que o estado mutado foi repassado para o método de salvamento do repository
            Mockito.verify(accountAssetRepository).save(accountAsset);
        }
    }

    @Nested
    @DisplayName("Teste do fluxo de transferência (transfer)")
    class transfer {
        @Test
        @DisplayName("Deve debitar a account de origem, creditar a account de destino, antes de salvar ambas")
        void deveTransferirComSucesso() {
            BigDecimal ammount = new BigDecimal("200.00");
            when(accountAssetRepository.findById(accountOrigId)).thenReturn(Optional.of(accountOrig));
            when(accountAssetRepository.findById(accountDestId)).thenReturn(Optional.of(accountDest));

            AccountAsset accountAssetResult = accountAssetService.transfer(accountOrigId, accountDestId, ammount);

            assertNotNull(accountAssetResult);
            //300 - 200 = 100
            assertEquals(new BigDecimal("100.00"), accountAssetResult.getAmount());
            //100 + 200 = 300
            assertEquals(new BigDecimal("300.00"), accountDest.getAmount());

            //garante a atomicidade da transaction
            verify(accountAssetRepository, times(1)).save(accountOrig);
            verify(accountAssetRepository, times(1)).save(accountDest);
        }
    }
}
