# System Design Document (SDD) — API Bancária
> Autor: Diná Andrade Lima  
> Escopo: Módulo de Depósitos, Saques e Transferências (EBANX)

Este documento formaliza as decisões arquiteturais, o modelo de dados e a estratégia de concorrência adotados para atender estritamente aos requisitos de consistência e simplicidade solicitados.

---

## 1. Visão Geral da Arquitetura

O sistema foi desenhado seguindo o padrão de **Camadas Desacopladas**, garantindo a separação total entre o protocolo de transporte (HTTP) e as regras fundamentais de negócio do domínio financeiro.
```
              [ Cliente HTTP ]
                     │  (JSON / Requisição)
                     ▼
┌─────────────────────────────────────────┐
│ Camada de Transporte (Controller / DTO) │
└────────────────────┬────────────────────┘
                     │  (Dados Tipados / BigDecimal)
                     ▼
┌─────────────────────────────────────────┐
│ Camada de Domínio / Regras (Service)    │
└────────────────────┬────────────────────┘
                     │  (Persistência Abstrata)
                     ▼
┌─────────────────────────────────────────┐
│     Camada de Armazenamento (Repo)      │
└─────────────────────────────────────────┘

```
### Justificativa de Componentes:
- **`AccountAssetController`**: Responsável estritamente pelo parsing do JSON, validação sintática via DTO imutável (`Record`) e tradução de exceções em códigos de status HTTP (`200`, `201`, `404`).
- **`AccountAssetService`**: Centraliza as regras financeiras. Não possui conhecimento sobre requisições HTTP, cabeçalhos ou URIs, tornando-o testável isoladamente de forma pura.
- **`AccountAssetRepository`**: Abstrai o mecanismo de armazenamento sob uma interface limpa.

---

## 2. Estratégia de Persistência & Alta Concorrência

Como a **durabilidade não era um requisito**, a infraestrutura foi simplificada, substituindo a complexidade de bancos de dados por um repositório centralizado em memória.

### Garantia de Thread-Safety:
Para mitigar condições de corrida (*Race Conditions*) decorrentes de múltiplas requisições paralelas disparadas pelo testador automatizado, o motor de armazenamento foi baseado na estrutura **`ConcurrentHashMap`**:
- **Operações Atômicas**: Leituras e escritas concorrentes não corrompem a tabela interna.
- **Mutação Segura**: O saldo das contas é atualizado via instâncias limpas do objeto de domínio, impedindo leituras sujas (*dirty reads*) durante o processamento de saques ou depósitos simultâneos.

---

## 3. Garantias Financeiras: Precisão e Atomicidade

### Precisão Numérica com `BigDecimal`:
Operações financeiras envolvendo moedas jamais devem utilizar tipos primitivos de ponto flutuante (`double` ou `float`) devido a erros acumulados de arredondamento. Utilizou-se **`BigDecimal`** em toda a cadeia de dados (do DTO ao Repositório), com validação explícita de escala e sinal (bloqueio rigoroso de valores negativos ou zerados).

### Atomicidade na Transferência:
A operação de transferência envolve duas mutações de estado que precisam ocorrer de forma consistente. O método `transfer` foi desenhado sequencialmente de forma atômica dentro do escopo do serviço:
1. O débito da conta de **origem** (`withdraw`) é executado primeiro. Se a origem não existir ou possuir saldo insuficiente, uma exceção é disparada imediatamente.
2. O fluxo é interrompido **antes** que qualquer alteração seja feita na conta de destino, impedindo a criação de dinheiro fantasma ou estados inconsistentes no sistema.

### Tratamento de Mensagens
Em projetos corporativos reais de grande porte, a prática recomendada envolve externalizar strings de erro em arquivos de propriedades externos (`messages.properties`) acionados pelo componente `MessageSource` no ecossistema Spring, viabilizando cenários de internacionalização.
No entanto, considerando o contexto específico e o peso atribuído à clareza de entrega, introduzir essa infraestrutura adicionaria uma complexidade desnecessária ao escopo atual do projeto.
Para resolver o acoplamento de textos brutos (*magic strings*) e reaproveitar as mensagens tanto nas validações de negócio quanto nas asserções da suíte de testes (princípio **DRY** - *Don't Repeat Yourself*), optou-se pelo design mais enxuto: a centralização em uma classe utilitária final de constantes (`AccountMessages`). Isso mantém o código limpo, flexível e imune a erros de digitação, sem inflar a arquitetura com recursos não solicitados.

---

## 4. Estratégia de Testes Multicamadas

A qualidade da entrega foi validada através de testes de cobertura de código, garantindo que o comportamento real do software reflita a especificação:

1. **Testes de Unidade de Domínio (`AccountAssetTest`)**: Validam a consistência das propriedades básicas e mutações simples do objeto de negócio.
2. **Testes de Serviço (`AccountAssetServiceTest`)**: Utilizam mocks controlados do Mockito para isolar a regra de negócio do repositório e forçar cenários excepcionais críticos (*Edge Cases*), como transferências para si mesmo e saques além do limite permitido.
3. **Testes de Integração End-to-End (`AccountAssetE2ETest`)**: Executam chamadas HTTP completas via `MockMvc` consumindo o repositório em memória real. Eles simulam com 100% de fidelidade a ordem cronológica exata das baterias de teste executadas pelo script externo do robô de testes.

---

## 5. Mapeamento Formal dos Contratos (API Spec)

Os endpoints foram intencionalmente expostos na raiz do servidor (`/balance`, `/event`, `/reset`) estritamente para garantir compatibilidade com as regras rígidas de parsing do *Ipkiss Tester*. Em um cenário real de produção, essas rotas seriam isoladas sob um contexto versionado (ex: `/api/v1/event`).

### [POST] `/reset`
- **Descrição**: Limpa integralmente o repositório em memória para execução de novas baterias de testes.
- **Códigos de Resposta**:
    - `200 OK`: Estado resetado. Corpo: `OK`

### [GET] `/balance`
- **Parâmetros**: `account_id` (String)
- **Códigos de Resposta**:
    - `200 OK`: Conta encontrada. Retorna valor bruto (ex: `20`).
    - `404 Not Found`: Conta inexistente. Retorna `0`.

### [POST] `/event`
- **Esquema de Entrada**:
  ```json
  {
    "type": "deposit" | "withdraw" | "transfer",
    "origin": "string (opcional)",
    "destination": "string (opcional)",
    "amount": "number"
  }
  ```
- **Códigos de Resposta**:
    - `201 Created`: Operação realizada com sucesso. Retorna o estado modificado da conta.
    - `404 Not Found`: Conta de origem inexistente ou saldo insuficiente. Retorna `0`.
