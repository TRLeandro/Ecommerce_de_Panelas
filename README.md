# Ecommerce de Panelas para Restaurantes

API REST em **Java** com **Quarkus**, voltada ao ecommerce B2B de panelas e utensílios para o setor gastronômico. O backend cobre catálogo, cadastro de clientes, pedidos, pagamentos, cupons, listas de desejo e operações administrativas, com autenticação JWT e controle de acesso por perfil.

---

## Tecnologias

| Tecnologia | Uso no projeto |
|------------|----------------|
| Java 25 | Linguagem e runtime |
| Quarkus 3.34 | Framework REST, injeção de dependência e build |
| PostgreSQL | Banco de dados em desenvolvimento/produção |
| Hibernate ORM / Panache | Persistência JPA |
| Redis | Cache (sessões e dados auxiliares) |
| SmallRye JWT | Autenticação e autorização |
| Argon2id | Hash de senhas (parâmetros configuráveis) |
| Quarkus Mailer | E-mails de recuperação e confirmação |
| Quarkus Scheduler | Expiração periódica de pagamentos |
| Hibernate Validator | Validação de DTOs |
| SmallRye OpenAPI | Documentação interativa da API |
| Docker / Docker Compose | Redis local e imagens de deploy |
| JUnit 5 + REST Assured | Testes de integração dos resources |

---

## Funcionalidades

### Catálogo e produto

- **Panelas** (`/panelas`): produto principal, com visão ecommerce (público) e CRUD administrativo.
- **Componentes** do produto: cores, materiais, fundos, sustentações e tampas (gestão restrita a `ADMIN` e `FUNCIONARIO`).
- **Categorias** e **coleções** para organização do catálogo.
- **Fornecedores** para rastreio de origem dos itens.

### Clientes e conta

- Cadastro **simples** e **completo** de usuários.
- Perfis: `ADMIN`, `FUNCIONARIO` e `CLIENTE`.
- Edição de dados, endereços e senha.
- **Endereços** com rotas administrativas e consulta por usuário autenticado.

### Compras

- **Pedidos** (`/pedidos`): criação de compras, histórico do cliente e gestão administrativa (status, filtros por usuário, cidade etc.).
- **Pagamentos** (`/pagamentos`): cartão, boleto e Pix por meio de um gateway plugável, com webhook, idempotência, expiração e estorno. Veja a seção [Pagamentos](#pagamentos).
- **Cupons de desconto** com regras de ativação e aplicação por valor mínimo.
- **Listas de desejo** com inclusão e remoção de panelas.

### Autenticação (`/auth`)

- Login com retorno de token JWT.
- Informações do usuário autenticado (`/auth/info`).
- Fluxos de alteração e recuperação de senha com envio de e-mail.

### Qualidade e erros

- Respostas de erro padronizadas (`ProblemDetail`) com mappers para validação, autorização, conflito de versão (optimistic lock) etc.
- Dados iniciais via `import.sql` ao subir com `drop-and-create`.
- Suíte de **testes de integração** por resource (H2 em memória no perfil de teste).

---

## Arquitetura

Camadas em `src/main/java/leepans/`:

```
leepans/
├── converter/     # Enums ↔ colunas do banco
├── dto/           # Request, response e DTOs de ecommerce
├── exception/     # Exceções de domínio e mappers JAX-RS
├── mapper/        # Entidade ↔ DTO
├── model/         # Entidades JPA
├── repository/    # Acesso a dados (Panache)
├── resource/      # Endpoints REST
└── service/
    ├── auth/      # Login, cache, e-mail
    ├── ecommerce/ # Regras de negócio do domínio
    └── pagamento/ # Gateway de pagamento, eventos pós-commit e agendamento
```

Separação de responsabilidades entre resource, service e repository, com DTOs na borda da API e regras concentradas nos services.

---

## API REST

Base URL em desenvolvimento: `http://localhost:8080`

| Prefixo | Domínio |
|---------|---------|
| `/auth` | Autenticação |
| `/usuarios` | Cadastro e gestão de usuários |
| `/panelas` | Produtos |
| `/categorias`, `/colecoes` | Organização do catálogo |
| `/cores`, `/materiais`, `/fundos`, `/sustentacoes`, `/tampas` | Componentes |
| `/fornecedores` | Fornecedores |
| `/enderecos` | Endereços |
| `/pedidos` | Pedidos |
| `/pagamentos` | Pagamentos |
| `/webhooks/pagamentos` | Notificações do gateway de pagamento |
| `/cupons-desconto` | Cupons |
| `/listas-desejo` | Listas de desejo |

Rotas administrativas costumam usar o sufixo `/admin` e exigem perfil `ADMIN` ou `FUNCIONARIO`, conforme o endpoint. Rotas de ecommerce e de cliente autenticado usam DTOs reduzidos e perfil `CLIENTE` quando aplicável.

Documentação interativa (com a aplicação em execução):

- Swagger UI: `http://localhost:8080/q/swagger-ui`
- OpenAPI: `http://localhost:8080/q/openapi`

Envie o token JWT no header `Authorization: Bearer <token>` nas rotas protegidas.

---

## Pagamentos

A loja nunca vê dados de cartão: o front-end tokeniza o cartão com o SDK do gateway e a API recebe só o token, que não é persistido. Do cartão ficam apenas bandeira, últimos 4 dígitos, parcelas e código de autorização. Pix e boleto são gerados pelo gateway.

Nenhum gateway real está integrado ainda. A implementação em uso é o `GatewayFake`, que simula o comportamento de um gateway.

### Fluxo

1. **Criar o pedido:** `POST /pedidos`. O pedido nasce em `AGUARDANDO_PAGAMENTO`, sem pagamento. Os preços vêm do catálogo e o endereço é copiado para o pedido.
2. **Iniciar o pagamento:** `POST /pedidos/{id}/pagamentos` com o header `Idempotency-Key`. Cartão aprovado já volta `PAGO`; Pix e boleto voltam `AGUARDANDO_PAGAMENTO` com o QR Code ou a linha digitável.
3. **Confirmação:** o gateway chama `POST /webhooks/pagamentos/{gateway}`. Com o fake, um administrador também pode usar `POST /pagamentos/admin/{id}/simular-pagamento`. O status é sempre reconsultado no gateway, nunca lido do corpo do webhook.
4. **Pago:** o pagamento e o pedido passam a `PAGO`. Depois do commit saem os e-mails, os itens deixam a lista de desejos e o cupom esgotado é desativado.

Um pedido pode ter várias tentativas, mas no máximo um pagamento ativo: iniciar outro cancela o anterior. Enquanto o Pix não é pago, o front pode acompanhar por `GET /pagamentos/{id}`.

Uma tarefa agendada expira os pagamentos vencidos. O pedido continua aberto para o cliente tentar de novo, até completar o prazo de `app.pedido.horas-para-cancelamento` sem pagamento ativo: nesse ponto ele é cancelado e o cupom é devolvido.

### Estados

| Status do pagamento | Significado | Vai para |
|---|---|---|
| `PENDENTE` | Criado, ainda sem resposta do gateway | `AGUARDANDO_PAGAMENTO`, `AUTORIZADO`, `PAGO`, `RECUSADO`, `CANCELADO` |
| `AGUARDANDO_PAGAMENTO` | Pix ou boleto gerado | `PAGO`, `EXPIRADO`, `CANCELADO` |
| `AUTORIZADO` | Valor reservado no cartão | `PAGO`, `RECUSADO`, `CANCELADO` |
| `PAGO` | Pagamento confirmado | `ESTORNADO`, `CONTESTADO` |
| `RECUSADO` | Recusado pelo gateway | final |
| `EXPIRADO` | Prazo vencido sem pagamento | final |
| `CANCELADO` | Substituído por outra tentativa, ou pedido cancelado | final |
| `ESTORNADO` | Valor devolvido; cancela o pedido | final |
| `CONTESTADO` | Chargeback aberto pelo cliente | final |

| Status do pedido | Vai para |
|---|---|
| `AGUARDANDO_PAGAMENTO` | `PAGO`, `CANCELADO` |
| `PAGO` | `EM_SEPARACAO`, `CANCELADO` |
| `EM_SEPARACAO` | `ENVIADO`, `CANCELADO` |
| `ENVIADO` | `ENTREGUE` |
| `ENTREGUE` | final |
| `CANCELADO` | final |

### Endpoints

| Método e rota | Perfil | Descrição |
|---|---|---|
| `POST /pedidos/{id}/pagamentos` | `CLIENTE` (dono) | Inicia o pagamento. Header `Idempotency-Key` obrigatório (422 sem ele). 201 ao criar, 200 com chave repetida, 503 se o gateway estiver fora do ar |
| `GET /pagamentos/{id}` | `CLIENTE` (dono) | Status atual, para polling do Pix |
| `GET /pagamentos/ecommerce` | autenticado | Pagamentos do usuário |
| `GET /pagamentos/admin`, `/admin/{id}`, `/admin/status/{status}` | `ADMIN`, `FUNCIONARIO` | Consultas administrativas |
| `GET /pagamentos/admin/{id}/transacoes` | `ADMIN`, `FUNCIONARIO` | Histórico de interações com o gateway |
| `POST /pagamentos/admin/{id}/estorno` | `ADMIN` | Estorna, cancela o pedido e devolve o cupom |
| `POST /pagamentos/admin/{id}/simular-pagamento` | `ADMIN`, `FUNCIONARIO` | Só com o gateway `fake` (404 nos demais) |
| `POST /pedidos/admin/{id}/separacao`, `/envio`, `/entrega` | `ADMIN`, `FUNCIONARIO` | Avançam o pedido |
| `POST /pedidos/admin/{id}/cancelamento` | `ADMIN`, `FUNCIONARIO` | Cancela o pedido; se já estiver pago, estorna |
| `POST /webhooks/pagamentos/{gateway}` | público, assinado | 200 ao processar ou repetir, 401 com assinatura inválida, 404 para outro gateway |

Corpo de `POST /pedidos/{id}/pagamentos`:

```json
{ "metodo": "CARTAO_CREDITO", "tokenCartao": "tok_ok", "parcelas": 3 }
```

`metodo` aceita `PIX`, `BOLETO`, `CARTAO_CREDITO` e `CARTAO_DEBITO`. `tokenCartao` é obrigatório para cartão. `parcelas` só vale para crédito; os demais métodos usam sempre 1.

**Removidos:** `PUT /pagamentos/admin/processar/{id}`, `PUT /pagamentos/cartao/{id}`, `PUT /pagamentos/boleto/{id}`, `PUT /pagamentos/pix/{id}`, `PATCH /pagamentos/admin/{id}`, `DELETE /pagamentos/admin/{id}` e `PATCH /pedidos/admin/{id}/status/{status}`.

### Configuração

| Propriedade | Padrão | Descrição |
|---|---|---|
| `app.pagamento.gateway` | `fake` | Implementação de `GatewayPagamento` em uso; valor desconhecido impede a aplicação de subir |
| `app.pagamento.webhook-secret` | `${PAGAMENTO_WEBHOOK_SECRET:dev-secret-trocar}` | Segredo do HMAC dos webhooks. Defina a variável fora do ambiente local |
| `app.pagamento.pix.expiracao-minutos` | `30` | Validade do Pix no `GatewayFake` |
| `app.pagamento.boleto.dias-vencimento` | `3` | Vencimento do boleto no `GatewayFake` |
| `app.pagamento.cartao.max-parcelas` | `12` | Máximo de parcelas no crédito |
| `app.pagamento.expiracao.intervalo` | `5m` | Intervalo da tarefa de expiração |
| `app.pedido.horas-para-cancelamento` | `24` | Prazo para cancelar pedido sem pagamento ativo |

### Testando com o GatewayFake

| `tokenCartao` | Resultado |
|---|---|
| `tok_ok` (ou qualquer outro valor) | Aprovado: `PAGO`, bandeira `VISA`, final `4242` |
| `tok_recusado` | `RECUSADO`, código `cc_rejected_insufficient_amount` |
| `tok_erro` | Gateway indisponível: 503, pagamento continua `PENDENTE` |

As cobranças do fake ficam em memória. Ao reiniciar a aplicação, uma cobrança desconhecida é tratada como `AGUARDANDO_PAGAMENTO`.

Para enviar um webhook à mão, use o `gatewayPaymentId` devolvido ao iniciar o pagamento:

```bash
SEGREDO=dev-secret-trocar
CORPO='{"id":"evt_1","paymentId":"fake_COLE_O_ID_AQUI","status":"PAGO"}'
ASSINATURA=$(printf '%s' "$CORPO" | openssl dgst -sha256 -hmac "$SEGREDO" | awk '{print $NF}')

curl -i -X POST http://localhost:8080/webhooks/pagamentos/fake \
  -H 'Content-Type: application/json' \
  -H "X-Signature: $ASSINATURA" \
  -d "$CORPO"
```

### Como integrar um gateway real

1. **Implemente `GatewayPagamento`** em `leepans.service.pagamento.gateway`, como bean `@ApplicationScoped`. `nome()` define o valor de configuração e a URL do webhook. A conversão de `BigDecimal` para centavos, quando o gateway exigir, fica só dentro desse adaptador.
2. **Mapeie os status** do gateway para `StatusGateway`. Recusa é um status, não uma exceção; erro técnico (rede, timeout, 5xx) lança `GatewayIndisponivelException`. Envie a `idempotencyKey` da solicitação ao gateway, se ele aceitar.
3. **Valide o webhook** em `interpretarWebhook` com o esquema de assinatura do gateway, lançando `AuthorizationException` quando ela não bater.
4. **Configure** `app.pagamento.gateway` com o `nome()` do adaptador e `PAGAMENTO_WEBHOOK_SECRET` com o segredo do gateway. Cadastre no painel dele a URL `https://sua-api/webhooks/pagamentos/{nome}`.
5. **Tokenize o cartão no front-end** com o SDK do gateway e envie o token em `tokenCartao`. Número, CVV e validade nunca devem chegar à API.

---

## Como executar

### Pré-requisitos

- JDK 25+
- Maven (ou use o wrapper `mvnw` / `mvnw.cmd`)
- PostgreSQL
- Redis (recomendado via Docker Compose)

### 1. Clonar o repositório

```bash
git clone https://github.com/UniLeandroTR/Ecommerce_de_Panelas.git
cd Ecommerce_de_Panelas
```

### 2. Variáveis de ambiente

Copie o arquivo de exemplo e ajuste os valores:

```bash
cp .env.example .env
```

Principais variáveis (ver `.env.example` para a lista completa):

- `DB_KIND`, `DB_USERNAME`, `DB_PASSWORD`, `DB_URL` — PostgreSQL
- `JWT_PUBLIC_KEY_LOCATION`, `JWT_SIGN_KEY_LOCATION`, `JWT_ISSUER` — JWT (chaves em `src/main/resources/META-INF/resources/`)
- `REDIS_HOSTS` — Redis
- `ARGON2_*` — parâmetros do hash de senha
- `MAILER_*` — SMTP para envio de e-mails
- `PAGAMENTO_WEBHOOK_SECRET` — segredo do HMAC dos webhooks de pagamento

O `application.properties` referencia essas variáveis; não commite credenciais reais.

### 3. Subir o Redis (opcional, recomendado)

```bash
docker compose up -d
```

### 4. Executar em modo desenvolvimento

```bash
./mvnw quarkus:dev
```

No Windows:

```cmd
mvnw.cmd quarkus:dev
```

A API ficará disponível em `http://localhost:8080`.

> Em desenvolvimento, o Hibernate está configurado com `drop-and-create` e carrega dados de exemplo via `src/main/resources/import.sql`. Ajuste `quarkus.hibernate-orm.database.generation` em `application.properties` quando for usar um banco persistente.

---

## Testes

```bash
./mvnw test
```

Os testes usam H2 em memória e configuração em `src/test/resources/application.properties`. Cobrem os resources REST com JWT de teste, as regras de pedidos e pagamentos com o `GatewayFake` real e o carregamento do `import.sql`.

---

## Docker

- `docker-compose.yml` — serviço Redis para desenvolvimento local.
- `src/main/docker/` — Dockerfiles JVM, legacy JAR e native para empacotamento da aplicação.

---

## Roadmap

- [x] Estrutura em camadas e domínio do ecommerce
- [x] CRUD de panelas, componentes, categorias, coleções e fornecedores
- [x] Usuários, endereços e autenticação JWT
- [x] Pedidos, pagamentos e cupons de desconto
- [x] Abstração de gateway de pagamento, com webhook e expiração
- [ ] Integração com um gateway de pagamento real
- [x] Listas de desejo
- [x] Testes de integração dos resources
- [x] Documentação OpenAPI
- [ ] Integração com frontend
- [ ] Deploy em ambiente cloud
- [ ] Pipeline CI/CD

---

## Contribuição

1. Faça um fork do projeto
2. Crie uma branch (`feature/sua-feature`)
3. Commit suas alterações
4. Abra um Pull Request

---

## Autor

**[Leandro Tavares Rosendo](https://github.com/Rosendoxx/Rosendoxx)**
