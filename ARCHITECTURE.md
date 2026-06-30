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
- **`AccountAssetController`**: Responsável por receber requisições, delegar todas as operações ao serviço e montar respostas de sucesso. Não captura exceções de negócio nem acessa diretamente o repositório.
- **`ApiExceptionHandler`**: Componente global baseado em `@RestControllerAdvice` que traduz `BusinessException` para o contrato do Ipkiss (`404` com corpo `0`).
- **`AccountAssetService`**: Centraliza as regras financeiras e o acesso ao repositório para consulta de saldo, depósito, saque, transferência e reset. Não possui conhecimento sobre requisições HTTP, cabeçalhos ou URIs.
- **`AccountValidatorService`**: Centraliza a validação sintática dos identificadores de conta, como presença, ausência de branco e formato numérico esperado.
- **`AccountAssetRepository`**: Abstrai o mecanismo de armazenamento sob uma interface limpa.

---

## 2. Armazenamento em Memória e Concorrência

Como a **durabilidade não era um requisito**, a infraestrutura foi simplificada, substituindo a complexidade de bancos de dados por um repositório centralizado em memória.

### Garantia de Thread-Safety:
Para proteger a estrutura interna do armazenamento durante requisições concorrentes disparadas pelo testador automatizado, o motor de armazenamento foi baseado na estrutura **`ConcurrentHashMap`**:
- **Segurança Estrutural**: Leituras e escritas concorrentes não corrompem a tabela interna do mapa.
- **Concorrência Local**: O `ConcurrentHashMap` oferece boa concorrência dentro de uma única JVM e atende ao requisitado.

Como essa estrutura, isoladamente, não garante atomicidade para operações compostas de leitura, alteração e gravação, os métodos públicos do `AccountAssetService` utilizam sincronização local. O monitor único do serviço protege consulta, depósito, saque, transferência e reset durante toda a seção crítica.

O estado de cada conta é representado por um `AccountAsset` imutável. Cada alteração produz uma nova instância, impedindo que referências retornadas pelo serviço modifiquem o conteúdo armazenado fora da seção sincronizada.

Essa garantia é restrita a uma única instância da aplicação. Ela não compartilha estado entre JVMs e não substitui uma transação ACID (Atomicidade, Consistência, Isolamento e Durabilidade) de um mecanismo persistente.

---

## 3. Garantias Financeiras: Precisão e Atomicidade Local

### Precisão Numérica com `BigDecimal`:
Operações financeiras envolvendo moedas jamais devem utilizar tipos primitivos de ponto flutuante (`double` ou `float`) devido a erros acumulados de arredondamento. Utilizou-se **`BigDecimal`** em toda a cadeia de dados (do DTO ao Repositório), com bloqueio rigoroso de valores negativos, zerados ou ausentes.

### Comportamento da Transferência:
A operação de transferência envolve duas alterações de estado que precisam ocorrer de forma consistente. Como a especificação não exige banco de dados nem controle transacional ACID, a atomicidade concorrente é garantida localmente pelo monitor do `AccountAssetService`:

1. Identificadores, valor, diferença entre origem e destino, existência da origem e saldo suficiente são validados antes da primeira gravação.
2. Os novos saldos de origem e destino são calculados em instâncias imutáveis.
3. As duas contas são gravadas enquanto o monitor do serviço permanece adquirido.
4. Consultas e outras mutações utilizam o mesmo monitor e, portanto, não observam a transferência entre o débito e o crédito.

Essa estratégia garante consistência para as regras de negócio e para requisições concorrentes processadas pela mesma JVM. Ela não oferece rollback para falhas de infraestrutura, durabilidade após reinicialização ou atomicidade distribuída entre múltiplas instâncias.

### Tratamento de Mensagens
Em projetos corporativos reais de grande porte, a prática recomendada envolve externalizar strings de erro em arquivos de propriedades externos (`messages.properties`) acionados pelo componente `MessageSource` no ecossistema Spring, viabilizando cenários de internacionalização.
No entanto, considerando o contexto específico e o peso atribuído à clareza de entrega, introduzir essa infraestrutura adicionaria uma complexidade desnecessária ao escopo atual do projeto.
Para resolver o acoplamento de textos brutos (*magic strings*) e reaproveitar as mensagens tanto nas validações de negócio quanto nas asserções da suíte de testes (princípio **DRY** - *Don't Repeat Yourself*), optou-se pela centralização em uma classe utilitária final de constantes (`AccountMessages`).

As falhas previstas são representadas por uma única `BusinessException`. Cada ocorrência recebe um `ErrorCode` estável, permitindo identificar programaticamente a causa sem criar uma classe para cada possível erro e sem depender do texto da mensagem. O código é interno e não modifica a resposta exigida pelo Ipkiss.

O `ApiExceptionHandler` trata exclusivamente `BusinessException`. Exceções de parsing e validações nativas do protocolo continuam sob o comportamento padrão do Spring, preservando `400 Bad Request` para JSON malformado e parâmetros obrigatórios ausentes. Não há captura genérica de `Exception`, evitando ocultar falhas inesperadas.

---

## 4. Estratégia de Testes Multicamadas

A qualidade da entrega foi validada através de testes de cobertura de código, garantindo que o comportamento real do software reflita a especificação:

1. **Testes de Unidade de Domínio (`AccountAssetTest`)**: Validam a consistência das propriedades básicas e mutações simples do objeto de negócio.
2. **Testes de Serviço (`AccountAssetServiceTest`)**: Utilizam mocks controlados do Mockito para isolar a regra de negócio do repositório e forçar cenários excepcionais críticos (*Edge Cases*), como transferências para si mesmo e saques além do limite permitido.
3. **Testes de Integração End-to-End (`AccountAssetE2ETest`)**: Executam chamadas HTTP completas via `MockMvc` consumindo o repositório em memória real. Eles reproduzem a ordem cronológica das baterias de teste executadas pelo script externo do robô de testes e incluem cenários adicionais de robustez, como saldo insuficiente e campos ausentes.
4. **Testes de Caracterização HTTP**: Fixam explicitamente as respostas atuais para JSON malformado, tipo de evento desconhecido e ausência de `account_id`, impedindo mudanças acidentais entre `400` e `404`.
5. **Testes Complementares de Robustez (`AccountAssetRobustnessE2ETest`)**: Reinicializam o estado antes de cada cenário para eliminar dependência de ordem. Cobrem transferências para contas existentes, preservação de saldo após falhas, reset intermediário, precisão decimal, IDs e valores inválidos, payloads inválidos e recuperação após uma operação recusada.
6. **Testes de Concorrência (`AccountAssetConcurrencyTest`)**: Utilizam o repositório real em memória e múltiplas threads coordenadas. Verificam ausência de atualizações perdidas em depósitos, impedimento de saldo negativo em saques, preservação do total financeiro em transferências e ausência de alteração após transferência recusada.

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
  - `404 Not Found`: Conta de origem inexistente, identificador inválido, saldo insuficiente ou payload financeiro inválido. Retorna `0`.
  - `400 Bad Request`: JSON malformado ou tipo de evento desconhecido.

---

## 6. Boas Práticas, Restrições do Contrato e Escopo do Projeto

Este projeto adota práticas de engenharia compatíveis com sua finalidade, mantendo conformidade estrita com o contrato exigido pelo *Ipkiss Tester*.

Nem todas as práticas normalmente recomendadas para APIs em produção são necessárias ou aplicáveis ao projeto atual. Algumas alterariam o contrato HTTP esperado pelo testador, enquanto outras introduziriam infraestrutura e complexidade sem requisito funcional ou operacional correspondente.

As decisões foram classificadas em três grupos:

1. Práticas já adotadas.
2. Práticas não adotadas por restrição do contrato.
3. Práticas válidas para produção, mas fora do escopo do projeto.

### 6.1. Práticas já adotadas

O projeto implementa:

- Separação entre Controller, Service e Repository.
- Regras de negócio isoladas da camada HTTP.
- Validação de identificadores e valores financeiros.
- Uso de `BigDecimal` para operações monetárias.
- Tratamento centralizado de falhas de negócio com `@RestControllerAdvice`.
- Códigos internos de erro por meio de `ErrorCode`.
- Uso de estrutura concorrente para armazenamento em memória.
- Maven Wrapper para execução reproduzível.
- Testes unitários, de serviço e End-to-End.
- Documentação dos endpoints e das decisões arquiteturais.
- Logging técnico padrão fornecido pelo Spring Boot.

#### Logs estruturados

APIs destinadas a ambientes produtivos normalmente devem possuir logs estruturados, correlação por request ID, registro de falhas técnicas e políticas de rotação e retenção.

O projeto mantém o logging técnico padrão fornecido pelo Spring Boot, que registra eventos como inicialização da aplicação, carregamento do contexto e determinadas falhas tratadas pelo Spring MVC, incluindo JSON malformado e parâmetros HTTP obrigatórios ausentes.

Exemplos:

```text
INFO  ... ApiApplication : Starting ApiApplication using Java 17...
INFO  ... ApiApplication : Started ApiApplication in 1.7 seconds
WARN  ... DefaultHandlerExceptionResolver :
Resolved [HttpMessageNotReadableException: JSON parse error...]
WARN  ... DefaultHandlerExceptionResolver :
Resolved [MissingServletRequestParameterException:
Required request parameter 'account_id' is not present]
```

Não foram implementados logs estruturados específicos da aplicação, como eventos de utilização, duração de requisições, operações financeiras, falhas de negócio estruturadas ou trilhas de auditoria.

Essa ausência é uma decisão consciente de escopo e não uma recomendação geral para APIs produtivas. Ela também não decorre da inexistência de banco de dados, pois persistência e observabilidade são responsabilidades independentes.

A aplicação foi construída para atender ao contrato delimitado do *Ipkiss Tester*. Não existe, no escopo atual, ambiente produtivo, operação distribuída, autenticação, SLA, plataforma de observabilidade, política de retenção ou requisito de auditoria.

Adicionar logs estruturados sem definir finalidade, campos permitidos, proteção de dados, armazenamento, rotação, retenção e consumidor desses logs criaria uma implementação incompleta de observabilidade sem requisito operacional correspondente.

Como consequência dessa decisão, o projeto não oferece correlação por request ID, consulta estruturada de eventos, métricas derivadas de logs ou investigação centralizada de incidentes.

Caso o projeto seja promovido para um ambiente operacional, logs estruturados deverão ser priorizados. Recomenda-se então implementar JSON estruturado, request ID, separação entre eventos HTTP, eventos de negócio e falhas técnicas, mascaramento de dados, rotação de arquivos, retenção limitada e agregação centralizada quando houver múltiplas instâncias.

Essa evolução pode ser realizada sem alterar os endpoints ou o contrato HTTP exigido pelo *Ipkiss Tester*.

### 6.2. Práticas não adotadas por restrição do contrato

#### Versionamento das rotas

Em uma API em ambiente real de procução, seria recomendável utilizar rotas versionadas, como:

```text
/api/v1/event
/api/v1/balance
/api/v1/reset
```

O *Ipkiss Tester*, entretanto, exige os endpoints diretamente na raiz:

```text
/event
/balance
/reset
```

Por esse motivo, não foi adicionado versionamento à URL. A decisão preserva a compatibilidade com o testador e não representa uma recomendação geral para APIs produtivas.

O projeto continua possuindo versionamento próprio no artefato Maven, independentemente do versionamento do contrato HTTP.

#### Contrato de erros

O retorno `404 Not Found` com corpo `0` é mantido para falhas de negócio porque faz parte do comportamento esperado pelo *Ipkiss Tester*.

Em uma API em ambine real de produção, seria preferível distinguir entrada inválida, recurso inexistente e operação recusada por regra de negócio, utilizando códigos HTTP e respostas estruturadas mais expressivas.

Alterar esse comportamento no projeto atual quebraria a compatibilidade com a especificação de homologação.

### 6.3. Práticas válidas para produção, mas fora do escopo do projeto

As práticas abaixo podem ser relevantes em uma API destinada à operação produtiva, mas não são necessárias no contexto atual:

- Logs JSON estruturados
- Correlação de requisições por request ID
- Métricas de utilização e desempenho
- Health checks operacionais
- Configuração separada por ambientes
- Gestão externa de segredos
- Políticas de timeout e encerramento controlado
- Headers adicionais de segurança
- Monitoramento e alertas
- Pipeline de integração e entrega contínua
- Políticas formais de compatibilidade e depreciação da API

A ausência desses recursos não significa que sejam inadequados e menos ainda,desnecessários em APIs de produção. Significa apenas que não há, neste projeto, ambiente operacional, consumidor de observabilidade, SLA ou requisito de infraestrutura que justifique sua implementação.

### 6.4. Itens que não se justificam no projeto atual

Seguindo estritamente o que foi solicitado e evitando introduzir comportamentos ou infraestrutura sem requisito correspondente, não se justificam neste momento:

- **Autenticação e autorização:** não fazem parte do contrato solicitado.
- **Rate limiting:** não foi solicitado e poderia interferir na bateria automatizada do *Ipkiss Tester*.
- **Idempotency keys:** exigiriam alteração no contrato e não são fornecidas pelo testador.
- **Banco de dados ou cache externo:** foram explicitamente excluídos do escopo; o estado deve permanecer em memória.
- **Mensageria e processamento assíncrono:** adicionariam complexidade sem requisito funcional.
- **Service discovery ou arquitetura distribuída:** não há múltiplos serviços ou instâncias que justifiquem essa infraestrutura.
- **Auditoria durável:** não existe requisito de retenção ou durabilidade do histórico das operações.
- **Versionamento obrigatório na URL:** seria incompatível com as rotas exigidas pelo testador.

### 6.5. Critério para evolução

Caso o projeto passe a atender consumidores reais ou seja promovido para um ambiente operacional, essas decisões deverão ser reavaliadas.

Nesse cenário, recomenda-se priorizar logs estruturados, request ID, métricas, health checks, segurança, versionamento de contrato e uma política explícita de retenção e proteção de dados.

Essas evoluções podem ser adicionadas preservando as regras de negócio. Quando necessário, as rotas atuais podem ser mantidas como compatibilidade legada, enquanto uma API versionada é disponibilizada em paralelo.

---

## 7. Escalabilidade: e se o sistema crescesse muito?

A resposta depende da dimensão de crescimento considerada. Mantendo as restrições atuais — estado exclusivamente em memória, ausência de banco de dados e execução em uma única JVM — a aplicação pode aumentar sua capacidade por meio de escalabilidade vertical e melhorias na concorrência local.

O `ConcurrentHashMap` oferece boa concorrência estrutural dentro de uma única JVM e atende ao requisitado. A atomicidade concorrente das operações financeiras é garantida separadamente por um monitor único no `AccountAssetService`.

O lock global foi adotado como a solução mais simples e segura para consultas, depósitos, saques, transferências e reset. Caso testes de carga demonstrem contenção relevante, a implementação pode evoluir para locks por conta ou locks segmentados. Transferências devem adquirir os locks das contas em ordem determinística para evitar deadlocks.

### 7.1. Por que não começar com lock por conta?

Locks por conta permitiriam que operações independentes fossem executadas simultaneamente, por exemplo:

```text
depósito na conta 100 || saque na conta 200
```

Entretanto, essa estratégia exigiria:

- Registro e remoção de locks.
- Ordem determinística na transferência.
- Tratamento de criação concorrente de contas.
- Coordenação especial com `/reset`.
- Prevenção de deadlocks.
- Mais testes e mais código.

Para o escopo atual, optou-se por garantir a correção com um lock global simples. A evolução para locks por conta somente se justificaria caso testes de carga demonstrassem contenção relevante.

Também seriam necessários limites explícitos para o número de contas em memória, monitoramento de heap, controle de sobrecarga e testes concorrentes. Essas medidas aumentam a capacidade de uma única instância, mas não eliminam seus limites físicos.

A aplicação não pode ser escalada horizontalmente apenas adicionando réplicas, pois cada JVM possuiria seu próprio estado. Requisições direcionadas a instâncias diferentes poderiam observar saldos divergentes.

Afinidade de sessão, arquivos compartilhados ou particionamento manual não forneceriam, isoladamente, consistência, recuperação de falhas ou atomicidade distribuída adequada ao domínio financeiro.

Caso o crescimento futuro exija múltiplas instâncias, alta disponibilidade ou recuperação do estado, será necessário revisar pelo menos uma das restrições atuais e adotar um mecanismo compartilhado com garantias apropriadas. Isso não faz parte do projeto solicitado.

Portanto, a arquitetura atual é escalável verticalmente dentro de uma única JVM e adequada ao escopo do *Ipkiss Tester*. Ela não deve ser apresentada como uma arquitetura distribuída ou horizontalmente escalável.

### 7.2. Impacto do crescimento sobre a observabilidade

O logging técnico padrão é suficiente para o escopo atual de desenvolvimento e homologação, mas não seria suficiente para uma operação de maior escala.

Com aumento de volume, concorrência ou quantidade de instâncias, seria necessário correlacionar requisições, identificar gargalos, acompanhar erros e distinguir eventos originados por cada processo.

Nesse cenário, os logs estruturados deixariam de ser apenas uma melhoria possível e passariam a ser um requisito operacional. Cada instância deveria produzir seus próprios logs estruturados, evitando escrita concorrente no mesmo arquivo. Em uma arquitetura distribuída, esses registros deveriam ser coletados e consultados por uma solução centralizada.

Portanto, a ausência atual de logs estruturados é aceitável apenas dentro do escopo delimitado do projeto. Ela deverá ser reavaliada caso mudem os requisitos de operação, escala, suporte ou auditoria.
