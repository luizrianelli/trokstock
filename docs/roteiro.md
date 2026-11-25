# Roteiro de estudo e demonstração

## 1. Execute e confira o contrato

Suba o Compose e confira a Swagger UI. Abra `V1__initial_schema.sql` para ver as chaves estrangeiras, restrições de unicidade e verificações que protegem o banco. Flyway aplica a migração na inicialização e Hibernate valida o esquema. Em uma entrevista, explique como isso deixa a criação do banco reproduzível.

## 2. Passe da interface ao banco

Entre como `admin`. Cadastre **Caneca** por R$ 12,50 e **Caderno** por R$ 10,00. Cadastre um cliente e registre uma entrada de 5 canecas e 2 cadernos. O saldo e o histórico devem refletir as duas entradas.

Abra `InventoryController`, `InventoryService` e `ProductRepository`. A requisição é validada no controller. O serviço carrega o produto com um bloqueio de escrita e registra saldo e movimento na mesma transação.

## 3. Demonstre o ciclo de um pedido

1. Crie um rascunho com 2 canecas e 1 caderno. O total deve ser R$ 35,00. Os saldos ainda são 5 e 2.
2. Confirme. Os saldos devem passar a 3 e 1. O dashboard deve refletir o pedido e o histórico deve exibir duas saídas.
3. Cancele. Os saldos voltam a 5 e 2, o histórico ganha dois estornos e o pedido sai do total confirmado.
4. Crie outro pedido com 1 caneca e 3 cadernos. A confirmação deve falhar e deixar os dois saldos originais. Cada item guarda seu preço unitário no momento da criação do rascunho.

Em `OrderService`, siga `confirm` até `changeStock`. O bloqueio no pedido serializa suas transições. Os produtos são carregados em ordem de ID em todas as transições de estoque do pedido. Um erro em qualquer item desfaz inclusive as alterações feitas nos itens anteriores. Repetir uma confirmação válida ou um cancelamento evita movimentações duplicadas.

## 4. Verifique a autorização

Saia e entre como `operador`. Você pode cadastrar e confirmar. As ações restritas são omitidas pela interface e a API responde 403 se o operador tentar executá-las com um token CSRF válido.

Ao testar por cliente HTTP, mantenha o cookie de sessão entre as chamadas. Use `GET /api/auth/csrf` para obter o nome do cabeçalho e o token, faça `POST /api/auth/login` com `username` e `password` em formulário URL-encoded e preserve o cookie atualizado. Consulte `GET /api/auth/me` para ver o papel. Obtenha um token atualizado antes de enviar JSON a uma rota de escrita.

## 5. Explique o que os testes verificam

`InventoryServiceTest` isola a regra de saldo com Mockito. `StockFlowIntegrationTest` usa PostgreSQL real em Testcontainers e passa pelas rotas com MockMvc, incluindo login, CSRF, permissão, baixa, estorno, rollback e repetição de ações. Compare o cenário automatizado com o roteiro manual para saber o que cada teste prova.
