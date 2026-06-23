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
import java.util.List;
import java.util.Optional;

/**
 * Testes de Componente e Regras de Negócio para a classe AccountAssetService.
 * Utiliza Mockito para simular e isolar as dependências da camada de armazenamento (Repository).
 * Foca na validação dos fluxos lógicos e matemáticos de depósitos (deposit), saques (withdraw) e transferências (transfer),
 * cobrindo exaustivamente cenários excepcionais críticos (Edge Cases) e fluxos de erro, como por exemplo, saldo insuficiente.
 */
@ExtendWith(MockitoExtension.class)
public class AccountAssetServiceTest {

    @Mock
    private AccountAssetRepository accountAssetRepository;
    @Mock
    private AccountValidatorService accountValidatorService;

    @InjectMocks
    private AccountAssetService accountAssetService;

    private final String accountOrigId = "200";
    private final String accountDestId = "300";
    private AccountAsset accountOrig;
    private AccountAsset accountDest;

    @BeforeEach
    void setUp() {
        //antes de cada teste, reatribui os valores
        accountOrig = new AccountAsset(accountOrigId, new BigDecimal("30.00"));
        accountDest = new AccountAsset(accountDestId, new BigDecimal("10.00"));
    }

    @Nested
    @DisplayName("Teste do fluxo de depósito (deposit)")
    class deposit {
        @Test
        @DisplayName("Deve lançar exceção se o valor (amount) for negativo")
        void deveLancarExcecaoQuandoAmountForNegativo() {
            BigDecimal amount = new BigDecimal("-5.00");

            var exception = assertThrows(IllegalArgumentException.class, () -> accountAssetService.deposit(accountOrigId, amount));

            assertEquals(AccountMessages.VALOR_DEPOSITO_POSITIVO, exception.getMessage());
        }

        @Test
        @DisplayName("Deve lançar exceção se o valor (amount) for zero")
        void deveLancarExcecaoQuandoAmountForZero() {
            BigDecimal amount = BigDecimal.ZERO;

            var exception = assertThrows(IllegalArgumentException.class, () -> accountAssetService.deposit(accountOrigId, amount));

            assertEquals(AccountMessages.VALOR_DEPOSITO_POSITIVO, exception.getMessage());
        }

        @Test
        @DisplayName("Não deve permitir valor (amount) zero ou negativo e deve lançar exceção")
        void naoDevePermitirDepositComValorZeroOuNegativo() {
            BigDecimal amount = new BigDecimal("-1.00");

            var exception = assertThrows(IllegalArgumentException.class, () -> accountAssetService.deposit(accountOrigId, amount));

            assertEquals(AccountMessages.VALOR_DEPOSITO_POSITIVO, exception.getMessage());
        }

        @Test
        @DisplayName("Deve depositar (deposit) com sucesso e criar a conta dinâmicamente, caso não exista")
        void deveFazerDepositComSucesso() {
            BigDecimal amount = new BigDecimal("5.00");
            when(accountAssetRepository.findById((accountOrigId))).thenReturn(Optional.of(accountOrig));
            when(accountAssetRepository.save(any(AccountAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AccountAsset accountAssetResult = accountAssetService.deposit(accountOrigId, amount);

            assertNotNull(accountAssetResult);
            //30 + 5 = 35
            assertEquals(new BigDecimal("35.00"), accountAssetResult.getAmount());
            //garante a persistência
            verify(accountAssetRepository, times(1)).save(accountOrig);
        }
    }

    @Nested
    @DisplayName("Teste do fluxo de saque (withdraw)")
    class withdraw {
        @Test
        @DisplayName("Deve lançar exceção se o valor (amount) do saque (withdraw) for negativo")
        void deveLancarExcecaoQuandoWithdrqwForNegativo() {
            BigDecimal amount = new BigDecimal("-10.00");

            var exception = assertThrows(IllegalArgumentException.class, () -> accountAssetService.withdraw(accountOrigId, amount));

            assertEquals(AccountMessages.VALOR_SAQUE_POSITIVO, exception.getMessage());
        }

        @Test
        @DisplayName("Deve lançar exceção se o valor (amount) do saque (withdraw) for zero")
        void deveLancarExcecaoQuandoWithdrawForZero() {
            BigDecimal amount = BigDecimal.ZERO;

            var exception = assertThrows(IllegalArgumentException.class, () -> accountAssetService.withdraw(accountOrigId, amount));

            assertEquals(AccountMessages.VALOR_SAQUE_POSITIVO, exception.getMessage());
        }

        @Test
        @DisplayName("Deve reduzir o saldo (amount) e salvar quando houver recursos suficientes")
        void deveFazerWithdrawComSucesso() {
            BigDecimal amount = new BigDecimal("5.00");
            when(accountAssetRepository.findById(accountOrigId)).thenReturn(Optional.of(accountOrig));
            when(accountAssetRepository.save(any(AccountAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

            AccountAsset accountAssetResult = accountAssetService.withdraw(accountOrigId, amount);

            assertNotNull(accountAssetResult);
            //30 - 5 = 25
            assertEquals(new BigDecimal("25.00"), accountAssetResult.getAmount());
            verify(accountAssetRepository, times(1)).save(accountOrig);
        }

        @Test
        @DisplayName("Deve lançar exceção e nunca pode salvar se o saldo (amount) for menor que o saque solicitado")
        void deveBarrarAmountInsuficiente() {
            //account tem apenas 300
            BigDecimal amount = new BigDecimal("600.00");
            when(accountAssetRepository.findById(accountOrigId)).thenReturn(Optional.of(accountOrig));

            //valida o saldo e lança a exceção
            var illegalStateException = assertThrows(IllegalStateException.class, () -> accountAssetService.withdraw(accountOrigId, amount));

            assertEquals(AccountMessages.SALDO_INSUFICIENTE, illegalStateException.getMessage());

            //garante que nada foi alterado/salvo
            verify(accountAssetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve diminuir o saldo (amount) da conta (account) e persistir")
        void deveDiminuirAmountDaAccountEPersistirNoRepository() {
            BigDecimal amountInit  = new BigDecimal("15.00");
            BigDecimal amount      = new BigDecimal("10.00");
            BigDecimal amountExpec = new BigDecimal("5.00");

            //verificando a mutação de estado, instanciando um objeto de domínio real
            AccountAsset accountAsset = new AccountAsset(accountOrigId, amountInit);
            Mockito.when(accountAssetRepository.findById(accountOrigId)).thenReturn(Optional.of(accountAsset));

            //testando a regra de negócio
            accountAssetService.withdraw(accountOrigId, amount);

            //garantindo que a regra de negócio alterou o saldo (amount) do objeto real consistentemente
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
        @DisplayName("Deve lançar exceção se a conta (account) de origem for igual à conta (account) de destino")
        void deveBarrarTransferParaMesmaConta() {
            BigDecimal amount = new BigDecimal("10.00");

            var exception = assertThrows(IllegalArgumentException.class, () -> accountAssetService.transfer(accountOrigId, accountOrigId, amount));

            assertEquals(AccountMessages.ORIGEM_IGUAL_DESTINO, exception.getMessage());
        }

        @Test
        @DisplayName("Deve lançar exceção e não alterar saldos (amount) se a origem não tiver fundos suficientes")
        void deveBarrarTransferSeOrigemNaoTiverAmount() {
            BigDecimal amountInvalido = new BigDecimal("500.00"); // Origem só tem 300
            when(accountAssetRepository.findById(accountOrigId)).thenReturn(Optional.of(accountOrig));

            assertThrows(IllegalStateException.class, () -> accountAssetService.transfer(accountOrigId, accountDestId, amountInvalido));

            //garante que o método save NUNCA foi chamado para nenhuma das contas, mantendo a consistência
            verify(accountAssetRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve debitar a conta (account) de origem, creditar a conta (account) de destino, antes de salvar ambas")
        void deveFazerTransferComSucesso() {
            BigDecimal ammount = new BigDecimal("20");
            when(accountAssetRepository.findById(accountOrigId)).thenReturn(Optional.of(accountOrig));
            when(accountAssetRepository.findById(accountDestId)).thenReturn(Optional.of(accountDest));

            when(accountAssetRepository.save(any(AccountAsset.class))).thenAnswer(invocation -> invocation.getArgument(0));

            List<AccountAsset> accountAssetResult = accountAssetService.transfer(accountOrigId, accountDestId, ammount);

            assertNotNull(accountAssetResult);
            //30 - 20 = 10
            assertEquals(0, new BigDecimal("10").compareTo(accountAssetResult.get(0).getAmount()));
            //10 + 20 = 30
            assertEquals(0, new BigDecimal("30").compareTo(accountAssetResult.get(1).getAmount()));

            //garante a atomicidade da transaction
            verify(accountAssetRepository, times(1)).save(accountOrig);
            verify(accountAssetRepository, times(1)).save(accountDest);
        }
    }
}
