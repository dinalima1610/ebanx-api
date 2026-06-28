# API Bancária (Módulo de Depósitos, Saques e Transferências)
> **Status:** Entrega Incremental — Pronta para Homologação com Ipkiss Tester


Esta aplicação foi desenvolvida seguindo boas práticas de engenharia de software.

## O que foi implementado nesta etapa
- **Separação de Responsabilidades:** Lógica de negócio isolada na camada Service, desacoplada de regras de transporte HTTP.
- **Persistência Eficiente em Memória:** Uso de estruturas thread-safe concorrentes (`ConcurrentHashMap`), eliminando a complexidade desnecessária de bancos de dados relacionais, atendendo estritamente ao critério de que durabilidade não era um requisito.
- **Validações de Entrada:** IDs de conta são validados no `AccountValidatorService`; regras financeiras como valores positivos e saldo suficiente ficam no `AccountAssetService`; o controller traduz exceções para respostas HTTP.
- **Testes Multicamadas:** Estratégia de testes abrangente, cobrindo validações de domínio, simulação de regras de negócio e testes integrados ponta a ponta (E2E) que reproduzem o roteiro do testador e cobrem cenários adicionais de robustez.


## ️ Como rodar o projeto e os testes

### Pré-requisitos
- Java 17 ou superior
- Maven Wrapper do projeto (`mvnw`/`mvnw.cmd`) ou Maven 3.9+ compatível
- Ngrok instalado (para exposição pública da API e homologação)

### Executar a Aplicação
```bash
./mvnw spring-boot:run
```

No Windows:
```powershell
.\mvnw.cmd spring-boot:run
```

### Executar a Suíte Interna de Testes
Para rodar a suíte completa de testes (unitários, de serviço com cenários de falha e integrados E2E), execute o comando abaixo no terminal:
```bash
./mvnw test
```

No Windows:
```powershell
.\mvnw.cmd test
```

## Próximo Passo: Homologação Externa (Ngrok & Ipkiss Tester)

Para disponibilizar a API na internet de forma segura e rodar a suíte automatizada de testes do EBANX Ninja (https://ipkiss.ebanx.ninja), siga o passo a passo abaixo:

### 1. Criar Conta no Ngrok
1. Acesse o site oficial do [ngrok](https://ngrok.com) e crie uma conta gratuita.
2. Acesse seu painel de controle e copie o seu token de autenticação (*Authtoken*).
3. No terminal do seu computador, configure o seu cliente ngrok com o token:
   ```bash
   ngrok config add-authtoken SEU_TOKEN_AQUI
   ```

### 2. Expor a API Local para a Internet
Com o projeto Spring Boot rodando localmente (na porta padrão `8080`), abra uma nova janela de terminal e execute o túnel HTTP:
```bash
ngrok http 8080
```
O ngrok gerará um endereço público seguro e temporário (ex: `https://ngrok-free.app`). Copie essa URL.

### 3. Executar os Testes no Ipkiss Tester
1. Acesse a plataforma oficial do testador automatizado disponibilizado pelo EBANX.
2. Cole a URL gerada pelo ngrok no campo de endereço do servidor (certifique-se de retirar barras extras ao final).
3. Inicie o conjunto de testes automatizados para validar as respostas da API em tempo real.

---

## Documentação da API & Conformidade Ipkiss Tester

Todos os contratos HTTP foram adaptados para responder na raiz do servidor, seguindo estritamente as assinaturas, payloads e códigos de status (`200`, `201`, `404`) exigidos pelo testador automatizado do EBANX Ninja.

### 1. Reset de Estado
Limpa integralmente o repositório em memória para execução de novas baterias de testes.
*   **Método:** `POST`
*   **Endpoint:** `/reset`
*   **Resposta (`200 OK`):** `OK`

### 2. Consulta de Saldo
*   **Método:** `GET`
*   **Endpoint:** `/balance?account_id={id}`
*   **Resposta - Conta Existente (`200 OK`):** `20` (Valor numérico bruto)
*   **Resposta - Conta Inexistente (`404 Not Found`):** `0`

### 3. Operações Financeiras Unificadas (Eventos)
*   **Método:** `POST`
*   **Endpoint:** `/event`

#### Exemplo Depósito (cria conta automaticamente caso não exista):
*   **Corpo da Requisição (Body):**
    ```json
    {
      "type": "deposit",
      "destination": "100",
      "amount": 10
    }
    ```
*   **Resposta (`201 Created`):**
    ```json
    {
      "destination": {
        "id": "100",
        "balance": 10
      }
    }
    ```

#### Exemplo Saque com sucesso:
*   **Corpo da Requisição (Body):**
    ```json
    {
      "type": "withdraw",
      "origin": "100",
      "amount": 5
    }
    ```

*   **Resposta (`201 Created`):**
    ```json
    {
      "origin": {
        "id": "100",
        "balance": 5
      }
    }
    ```

#### Exemplo Saque com saldo insuficiente:
Considerando uma conta existente com saldo menor que o valor solicitado, a API retorna o mesmo contrato de erro usado para operações financeiras não atendidas.
*   **Corpo da Requisição (Body):**
    ```json
    {
      "type": "withdraw",
      "origin": "100",
      "amount": 999
    }
    ```
*   **Resposta (`404 Not Found`):** `0`

#### Exemplo Transferência (executada de forma atômica):
*   **Corpo da Requisição (Body):**
    ```json
    {
      "type": "transfer",
      "origin": "100",
      "amount": 15,
      "destination": "300"
    }
    ```
*   **Resposta (`201 Created`):**
    ```json
    {
      "origin": {
        "id": "100",
        "balance": 0
      },
      "destination": {
        "id": "300",
        "balance": 15
      }
    }
    ```

---

## Principais Decisões Técnicas e de Arquitetura & Design de Rotas
1. **Tipos e Valores:** Uso exclusivo de `BigDecimal` para todas as operações financeiras em toda a cadeia de dados (Service, Controller e DTO), mitigando problemas clássicos de imprecisão de ponto flutuante (`double`/`float`).
2. **Design de Rotas na Raiz:** Em cenários corporativos reais, os endpoints seriam obrigatoriamente isolados sob contextos de negócio e versionados (ex: `/api/v1/balance`). Optou-se por expor os recursos `/event`, `/balance` e `/reset` diretamente na raiz do servidor estritamente para garantir compatibilidade com as regras de parsing e concatenação rígidas do script automatizado do `Ipkiss Tester`.
3. **Flexibilidade do Contrato:** O desacoplamento total entre a lógica de domínio (`AccountAssetService`) e os controladores de transporte HTTP garante que, caso uma nova versão da API necessite de padrões corporativos como `/v2/`, a refatoração envolverá apenas anotações de rota, sem qualquer impacto nas regras financeiras de estado.
4. **Armazenamento de Dados:** Substituição de infraestruturas relacionais pesadas por uma estratégia baseada em `ConcurrentHashMap`. Isso garante consistência estrita de estado, já que persistência durável não era um requisito.
5. **Abordagem Abrangente de Testes:** A aplicação conta com três níveis distintos de testes:
    - **Testes de Domínio:** Focados em validar o isolamento e corretude do estado das entidades.
    - **Testes de Serviço (Edge Cases):** Mockados com o Mockito para forçar e tratar cenários excepcionais como saques e transferências com saldo insuficiente ou transferências para uma mesma conta.
    - **Testes Integrados E2E:** Usando `MockMvc` com persistência em memória para reproduzir integralmente e sequencialmente as etapas estipuladas na especificação do `Ipkiss Tester`, além de cobrir cenários adicionais de robustez como saldo insuficiente e campos ausentes.


## Status do Projeto e Próximos Passos (Roadmap)

Este repositório segue uma estratégia de desenvolvimento incremental (boas práticas de engenharia de software).

- **[X] Etapa 1:** Estabilização do core de negócios (módulo de Consulta de Saldo, Depósitos, Saques, Transferências em `AccountAssetService`), persistência em banco de dados MySQL 8 com população automatizada via `data.sql` e testes unitários de comportamento real.
- **[X] Etapa 2:** Refatoração dos contratos da camada HTTP para alinhamento estrito com a especificação de testes da plataforma (rotas `/event`, `/balance` e `/reset`) e remoção de persistência pesada. Blindagem do projeto com testes de integração *Edge Cases* e estruturação de testes End-to-End (E2E) locais usando MockMvc.
- **[X] Etapa 3 (Atual):** Homologação externa na plataforma de testes utilizando exposição segura de túnel via Ngrok com 100% de sucesso na plataforma `Ipkiss Tester` via Ngrok. Criação da documentação formal de arquitetura (`ARCHITECTURE.md`) detalhando as decisões de design, padrões de concorrência e o System Design Document (SDD).

---

## Autor

Desenvolvido por **Diná Andrade Lima**
- **LinkedIn:** https://www.linkedin.com/in/diná-andrade-lima/
- **GitHub:** [@dinalima1610](https://github.com/dinalima1610)
