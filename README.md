# API Bancária (Módulo de Depósitos, Saques e Transferências)
> **Status:** Entrega Incremental — Etapa 1: Core de Negócios & Persistência 🚀


Esta é a primeira entrega incremental do projeto, focada na estabilização da lógica de negócio de depósitos, tratamento de erros críticos e consistência de estado.

## O que foi implementado nesta etapa
- **Separação de Responsabilidades:** Lógica de negócio isolada na camada Service, desacoplada de regras HTTP.
- **Validações de Entrada:** Bloqueio de valores negativos/zerados e verificação de contas existentes/válidas.
- **Testes de Comportamento Real:** Testes que validam o efeito colateral no estado do domínio (`AccountAsset`) e garantem a persistência do saldo atualizado.

## ️ Como rodar o projeto e os testes

### Pré-requisitos
- Java 17 ou superior
- Maven 4.x
- **Banco de Dados:** MySQL 8.x instalado e rodando


### Armazenamento de Dados (Persistência)
Para esta etapa executável, a aplicação está configurada para utilizar:
- **Banco de Dados:** MySQL 8
- **Estratégia de Inicialização:** As tabelas e relacionamentos são gerados automaticamente pelo Hibernate (`ddl-auto=update`).
- **Massa de Dados (Seed):** O banco é populado automaticamente com contas de teste (`987654321X` com saldo e `1234567890` zerada) na primeira inicialização da aplicação, através do script interno `data.sql`.

⚠️ **Pré-requisito importante:** O Spring Boot não criará o schema automaticamente se ele não existir no seu servidor local.

**Execute o script `init.sql` no seu MySQL antes de subir a aplicação** para garantir a criação correta do banco de dados `ebanx_api`.


### Executar a Aplicação
```bash
mvn spring-boot:run
```

### Executar os testes unitários e de comportamento
Para rodar a suíte de testes implementada nesta etapa, execute o comando abaixo no terminal:
```bash
mvn test
```

## Documentação da API (Contratos HTTP)

Todas as operações financeiras de escrita e leitura seguem um padrão de contrato uniforme e consistente. Substitua `{{baseUrl}}` pela URL correspondente ao ambiente desejado (ex: Desenvolvimento, Homologação ou Produção).

### 1. Consulta de Saldo
Retorna o estado atualizado de uma conta de forma puramente consultiva, sem efeitos colaterais.

*   **Método:** `GET`
*   **Endpoint:** `/api/v1/balance/{accountId}`
*   **Exemplo de Caminho:** `/api/v1/balance/987654321X`
*   **Resposta (`200 OK`):**
    ```json
    {
      "accountId": "987654321X",
      "amount": 250.00,
      "version": 8
    }
    ```

### 2. Evento: Depósito
Incrementa de forma consistente o saldo da conta informada. O valor enviado deve ser obrigatoriamente positivo.

*   **Método:** `POST`
*   **Endpoint:** `/api/v1/event/D/{accountId}`
*   **Exemplo de Caminho:** `/api/v1/event/D/987654321X`
*   **Corpo da Requisição (Body):**
    ```json
    {
      "amount": 450.00
    }
    ```
*   **Resposta (`200 OK`):**
    ```json
    {
      "accountId": "987654321X",
      "amount": 200.00,
      "version": 1
    }
    ```

### 3. Evento: Saque
Deduz o valor do saldo da conta informada, respeitando a validação de fundos/saldo insuficiente.

*   **Método:** `POST`
*   **Endpoint:** `/api/v1/event/W/{accountId}`
*   **Exemplo de Caminho:** `/api/v1/event/W/987654321X`
*   **Corpo da Requisição (Body):**
    ```json
    {
      "amount": 200.00
    }
    ```
*   **Resposta (`200 OK`):**
    ```json
    {
      "accountId": "987654321X",
      "amount": 100.00,
      "version": 3
    }
    ```

### 4. Evento: Transferência
Realiza a movimentação financeira debitando da conta de origem (passada no caminho) e creditando na conta de destino (passada no corpo). Operação executada de forma atômica.

*   **Método:** `POST`
*   **Endpoint:** `/api/v1/event/T/{accountId}`
*   **Exemplo de Caminho:** `/api/v1/event/T/987654321X`
*   **Corpo da Requisição (Body):**
    ```json
    {
      "accountDestId": "1234567890",
      "amount": 200.00
    }
    ```
*   **Resposta (`200 OK`):**
    ```json
    {
      "accountId": "987654321X",
      "amount": 0.00,
      "version": 2
    }
    ```

## Principais Decisões Técnicas
1. **Tipos e Valores:** Uso exclusivo de `BigDecimal` para todas as operações financeiras, evitando problemas de precisão de ponto flutuante (`double`/`float`).
2. **Abordagem de Testes:** Optou-se por testes unitários focados no comportamento do domínio. Os mocks foram utilizados estritamente para simular a infraestrutura (banco de dados/validadores externos), enquanto a mutação do saldo e as regras de negócio foram validadas em instâncias reais de objetos para evitar falsos-positivos.
3. **Imutabilidade e Estado:** Retornos HTTP padronizados contendo a versão do registro (`version`), estruturando o terreno para concorrência e consistência de estado.


## Status do Projeto e Próximos Passos (Roadmap)

Este repositório segue uma estratégia de desenvolvimento incremental (boas práticas de engenharia de software).

- **[X] Etapa 1 (Atual):** Estabilização do core de negócios (módulo de Consulta de Saldo, Depósitos, Saques, Transferências em `AccountAssetService`), persistência em banco de dados MySQL 8 com população automatizada via `data.sql` e testes unitários de comportamento real.
- **[ ] Etapa 2 (Próxima):** Refatoração dos contratos da camada HTTP (`AccountAssetController`) para alinhamento estrito com a especificação de testes automatizados da plataforma (rotas `/event`, `/balance` e `/reset`), inclusão dos fluxos de saque/transferência e exposição via túnel HTTP (ngrok).


---

## Autor

Desenvolvido por **Diná Andrade Lima**
- **LinkedIn:** https://www.linkedin.com/in/diná-andrade-lima/
- **E-mail:** dinalima.dal@gmail.com
- **GitHub:** [@dinalima1610](https://github.com/dinalima1610)
