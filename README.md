# StockFlow

Controle de pedidos e estoque para uma pequena empresa com um único estoque. O React consome uma API REST em Java e o PostgreSQL guarda cadastros, pedidos, saldos e movimentações.

## Executar

1. Instale e inicie o Docker com o Docker Compose v2 ou superior.
2. Na raiz, copie `.env.example` para `.env`. No PowerShell, use `Copy-Item .env.example .env`. No Bash, use `cp .env.example .env`.
3. Execute `docker compose up --build -d` e acompanhe a inicialização com `docker compose logs -f api`.
4. Abra [StockFlow](http://localhost:3000). Use `admin` com `ADMIN_PASSWORD` ou `operador` com `OPERATOR_PASSWORD`, definidos no `.env`. A [Swagger UI](http://localhost:8080/swagger-ui/index.html) e o [contrato OpenAPI](http://localhost:8080/v3/api-docs) estão na porta da API.

Use senhas próprias se a instância estiver acessível a terceiros. O `.env` fica fora do Git. Para parar, execute `docker compose down`. O volume PostgreSQL preserva os dados.

## Stack

| Componente | Versão |
| --- | --- |
| Java | 21 |
| Spring Boot | 3.5.16 |
| Maven Wrapper / Maven | 3.3.4 / 3.9.12 |
| springdoc-openapi | 2.10.1 |
| PostgreSQL | 17.10 |
| Node.js usado pelo Docker e CI | 22.22.0 |
| React / React Router | 19.2.7 / 7.18.7 |
| TypeScript / Vite / Bootstrap | 5.9.3 / 7.3.8 / 5.3.8 |

O BOM do Spring Boot fixa as versões gerenciadas, incluindo Spring Web, JPA, Bean Validation, Security, Flyway, driver PostgreSQL, JUnit, Mockito e Testcontainers. O `package-lock.json` fixa a árvore npm. O Java 21 atende aos requisitos do Spring Boot 3.5 e o springdoc 2.10 é compatível com essa linha. Node 22.22 atende ao mínimo da linha 22 exigido pelo Vite 7. [Requisitos Spring Boot](https://docs.spring.io/spring-boot/3.5/system-requirements.html) · [Matriz springdoc](https://springdoc.org/v2/) · [Requisitos Vite 7](https://main.vitejs.dev/blog/announcing-vite7)

## Estrutura

- `backend/` contém o monólito, organizado pelas funcionalidades `product`, `customer`, `inventory`, `order`, `auth`, `dashboard` e `common`. Cada controller é um arquivo independente. Os serviços concentram operações de estoque e pedidos.
- `frontend/` contém as páginas React, as rotas e o módulo de comunicação `src/api.ts`.
- `docs/` contém o [roteiro de estudo e demonstração](docs/roteiro.md).
- `.github/workflows/ci.yml` compila e testa o backend com PostgreSQL em Testcontainers e roda `npm ci` e o build do frontend.

## Desenvolvimento sem rebuild dos contêineres de aplicação

Suba apenas o banco em uma instalação local nova com `docker compose up -d db`. Exporte `DB_USER`, `DB_PASSWORD`, `ADMIN_PASSWORD` e `OPERATOR_PASSWORD` no terminal do backend usando os valores do `.env`. No PowerShell, cada variável pode ser definida com `$env:DB_USER = 'stockflow'` e o equivalente para as outras. Por padrão a URL é `jdbc:postgresql://localhost:5432/stockflow`.

- Em `backend/`, rode `./mvnw spring-boot:run` no Bash ou `.\mvnw.cmd spring-boot:run` no PowerShell.
- Em `frontend/`, rode `npm ci` e `npm run dev`. Abra [Vite](http://localhost:5173). O proxy do Vite entrega `/api` pela mesma origem do frontend. O Nginx faz o mesmo no Compose.
- Em `backend/`, use `./mvnw verify` ou `.\mvnw.cmd verify` com Docker ativo. Para executar apenas o teste unitário, acrescente `-Dtest=InventoryServiceTest` ao comando Maven.
- Em `frontend/`, use `npm run build` para checar tipos e gerar o bundle.

## Permissões

| Operação | Administrador | Operador |
| --- | --- | --- |
| Consultar dashboard, cadastros, saldos, movimentações e pedidos | Sim | Sim |
| Cadastrar clientes e produtos, criar e confirmar pedidos | Sim | Sim |
| Registrar entrada e cancelar pedidos | Sim | — |

As duas contas são configuradas por variáveis de ambiente e registradas em memória com senhas codificadas no startup. Spring Security usa sessão, cookie HttpOnly e CSRF. O frontend consulta `GET /api/auth/csrf` antes de cada escrita e envia o token no cabeçalho retornado. Para usar o Swagger, entre antes pelo frontend usando o mesmo hostname e obtenha o token pelo endpoint CSRF para enviar nas operações de escrita. Consulte o [roteiro](docs/roteiro.md) para praticar com requisições.

O escopo contempla uma empresa e um estoque. Pagamentos, emissão fiscal, frete, múltiplos depósitos e marketplaces estão fora desta versão.
