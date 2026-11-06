-- Script de importação para popular todas as entidades do projeto
-- Cada entidade possui pelo menos 3 registros

-- Categorias
insert into Categoria (tipo, data_cadastro, version) values ('Frigideira', CURRENT_DATE, 0);
insert into Categoria (tipo, data_cadastro, version) values ('Panela de Pressão', CURRENT_DATE, 0);
insert into Categoria (tipo, data_cadastro, version) values ('Caçarola', CURRENT_DATE, 0);

-- Coleções
insert into Colecao (nome, data_cadastro, version) values ('Clássica', CURRENT_DATE, 0);
insert into Colecao (nome, data_cadastro, version) values ('Gourmet', CURRENT_DATE, 0);
insert into Colecao (nome, data_cadastro, version) values ('Avançada', CURRENT_DATE, 0);

-- Cores
insert into Cor (nome, data_cadastro, version) values ('Vermelho', CURRENT_DATE, 0);
insert into Cor (nome, data_cadastro, version) values ('Preto', CURRENT_DATE, 0);
insert into Cor (nome, data_cadastro, version) values ('Inox', CURRENT_DATE, 0);

-- Fornecedores
insert into Fornecedor (nome, telefone, cnpj, data_cadastro, version) values ('Panelas Brasil', '(11) 4002-8922', '12.345.678/0001-01', CURRENT_DATE, 0);
insert into Fornecedor (nome, telefone, cnpj, data_cadastro, version) values ('Cozinha Pro', '(21) 5555-1234', '98.765.432/0001-09', CURRENT_DATE, 0);
insert into Fornecedor (nome, telefone, cnpj, data_cadastro, version) values ('Utensílios Prime', '(31) 3333-6789', '45.987.123/0001-55', CURRENT_DATE, 0);

-- Materiais
insert into Material (nome, data_cadastro, version) values ('Alumínio', CURRENT_DATE, 0);
insert into Material (nome, data_cadastro, version) values ('Aço Inox', CURRENT_DATE, 0);
insert into Material (nome, data_cadastro, version) values ('Cerâmica', CURRENT_DATE, 0);

-- Qualidades dos materiais
insert into material_qualidades (material_id, qualidades) values (1, 'Leve');
insert into material_qualidades (material_id, qualidades) values (1, 'Boa condutividade térmica');
insert into material_qualidades (material_id, qualidades) values (2, 'Resistente');
insert into material_qualidades (material_id, qualidades) values (2, 'Durável');
insert into material_qualidades (material_id, qualidades) values (3, 'Antiaderente');
insert into material_qualidades (material_id, qualidades) values (3, 'Fácil limpeza');

-- Componentes (superclasse Componente)
insert into Componente (peso, data_cadastro, version) values (0.9, CURRENT_DATE, 0);
insert into Componente (peso, data_cadastro, version) values (1.2, CURRENT_DATE, 0);
insert into Componente (peso, data_cadastro, version) values (1.5, CURRENT_DATE, 0);
insert into Componente (peso, data_cadastro, version) values (0.4, CURRENT_DATE, 0);
insert into Componente (peso, data_cadastro, version) values (0.5, CURRENT_DATE, 0);
insert into Componente (peso, data_cadastro, version) values (0.6, CURRENT_DATE, 0);
insert into Componente (peso, data_cadastro, version) values (0.8, CURRENT_DATE, 0);
insert into Componente (peso, data_cadastro, version) values (0.7, CURRENT_DATE, 0);
insert into Componente (peso, data_cadastro, version) values (0.9, CURRENT_DATE, 0);

-- Fundos
insert into Fundo (id, espessura, isantiaderente) values (1, 2.8, true);
insert into Fundo (id, espessura, isantiaderente) values (2, 3.2, false);
insert into Fundo (id, espessura, isantiaderente) values (3, 2.5, true);

-- Sustentações
insert into Sustentacao (id, tamanhoemcm, quantidade, codigo_tipo_sustentacao) values (4, 12, 1, 2);
insert into Sustentacao (id, tamanhoemcm, quantidade, codigo_tipo_sustentacao) values (5, 15, 2, 1);
insert into Sustentacao (id, tamanhoemcm, quantidade, codigo_tipo_sustentacao) values (6, 10, 1, 2);

-- Tampas
insert into Tampa (id, isdepressao) values (7, false);
insert into Tampa (id, isdepressao) values (8, true);
insert into Tampa (id, isdepressao) values (9, false);

-- Associação componentes <-> materiais
insert into componente_material (componente_id, material_id) values (1, 1);
insert into componente_material (componente_id, material_id) values (1, 2);
insert into componente_material (componente_id, material_id) values (2, 2);
insert into componente_material (componente_id, material_id) values (2, 3);
insert into componente_material (componente_id, material_id) values (3, 1);
insert into componente_material (componente_id, material_id) values (4, 1);
insert into componente_material (componente_id, material_id) values (5, 2);
insert into componente_material (componente_id, material_id) values (6, 3);
insert into componente_material (componente_id, material_id) values (7, 2);
insert into componente_material (componente_id, material_id) values (8, 3);
insert into componente_material (componente_id, material_id) values (9, 1);

-- Panelas
insert into Panela (modelo, preco, peso, capacidadelitros, descricao, isinducao, id_cor, id_material_principal, id_colecao, id_fundo, id_sustentacao, id_tampa, id_fornecedor, codigo_tamanho, id_categoria, data_cadastro, version) values
  ('Classic 24', 24990, 1.8, 3.5, 'Panela clássica com fundo triplo.', true, 3, 2, 1, 1, 4, 7, 1, 1, 1, CURRENT_DATE, 0),
  ('PressurePro', 49990, 2.4, 5.0, 'Panela de pressão com tampa de segurança.', false, 1, 1, 2, 2, 5, 8, 2, 2, 2, CURRENT_DATE, 0),
  ('CookPlus', 32990, 2.0, 4.2, 'Panela multiuso com acabamento inox.', true, 2, 2, 3, 3, 6, 9, 3, 3, 3, CURRENT_DATE, 0);

-- Regiões
insert into Regiao (nome, data_cadastro, version) values ('Norte', CURRENT_DATE, 0);
insert into Regiao (nome, data_cadastro, version) values ('Nordeste', CURRENT_DATE, 0);
insert into Regiao (nome, data_cadastro, version) values ('Centro-Oeste', CURRENT_DATE, 0);
insert into Regiao (nome, data_cadastro, version) values ('Sudeste', CURRENT_DATE, 0);
insert into Regiao (nome, data_cadastro, version) values ('Sul', CURRENT_DATE, 0);

-- Estados
insert into Estado (nome, sigla, id_regiao, data_cadastro, version) values ('São Paulo', 'SP', 4, CURRENT_DATE, 0);
insert into Estado (nome, sigla, id_regiao, data_cadastro, version) values ('Rio de Janeiro', 'RJ', 4, CURRENT_DATE, 0);
insert into Estado (nome, sigla, id_regiao, data_cadastro, version) values ('Minas Gerais', 'MG', 4, CURRENT_DATE, 0);
insert into Estado (nome, sigla, id_regiao, data_cadastro, version) values ('Tocantins', 'TO', 1, CURRENT_DATE, 0);

-- Cidades
insert into Cidade (nome, id_estado, data_cadastro, version) values ('São Paulo', 1, CURRENT_DATE, 0);
insert into Cidade (nome, id_estado, data_cadastro, version) values ('Rio de Janeiro', 2, CURRENT_DATE, 0);
insert into Cidade (nome, id_estado, data_cadastro, version) values ('Belo Horizonte', 3, CURRENT_DATE, 0);
insert into Cidade (nome, id_estado, data_cadastro, version) values ('Palmas', 4, CURRENT_DATE, 0);

-- Endereços
insert into Endereco (logradouro, numero, complemento, bairro, cep, id_cidade, data_cadastro, version) values ('Rua das Flores', '123', 'Apto 101', 'Centro', '01234-567', 1, CURRENT_DATE, 0);
insert into Endereco (logradouro, numero, complemento, bairro, cep, id_cidade, data_cadastro, version) values ('Avenida Paulista', '1000', null, 'Bela Vista', '01311-100', 1, CURRENT_DATE, 0);
insert into Endereco (logradouro, numero, complemento, bairro, cep, id_cidade, data_cadastro, version) values ('Rua Oscar Freire', '500', null, 'Cerqueira César', '01426-100', 1, CURRENT_DATE, 0);
insert into Endereco (logradouro, numero, complemento, bairro, cep, id_cidade, data_cadastro, version) values ('Avenida Rio Branco', '156', null, 'Centro', '20040-020', 2, CURRENT_DATE, 0);
insert into Endereco (logradouro, numero, complemento, bairro, cep, id_cidade, data_cadastro, version) values ('Rua Gonçalves Dias', '45', 'Sala 2', 'Centro', '20040-030', 2, CURRENT_DATE, 0);
insert into Endereco (logradouro, numero, complemento, bairro, cep, id_cidade, data_cadastro, version) values ('Avenida Getúlio Vargas', '1200', null, 'Savassi', '30130-100', 3, CURRENT_DATE, 0);
insert into Endereco (logradouro, numero, complemento, bairro, cep, id_cidade, data_cadastro, version) values ('Rua Espírito Santo', '789', null, 'Centro', '30140-071', 3, CURRENT_DATE, 0);

-- Usuários
insert into Usuario (login, senha_hash, perfil, data_cadastro, version) values ('Leandro', '$argon2id$v=19$m=65536,t=3,p=4$upAxp+68yN5Q+2Ttn6PiUg$mtcho1Pl1uRVdr/IizjBPzPs9mH9kp12r4QDj4ZklRI', 'ADMIN', CURRENT_DATE, 0);
insert into Usuario (login, senha_hash, perfil, data_cadastro, version) values ('Gustavo', '$argon2id$v=19$m=65536,t=3,p=4$1fWVYmlX/DbiEuHP930ZmA$mtQ4YUxXawl6JAb9m7HsHUQSpR6V+uij+LnfTV8460k', 'FUNCIONARIO', CURRENT_DATE, 0);
insert into Usuario (login, senha_hash, perfil, data_cadastro, version) values ('Pedro', '$argon2id$v=19$m=65536,t=3,p=4$le+GEcgt+PGyT+Q4Yep+4w$NYEpQ9ZJwpqKTON3e2QqpSdB20dWdODRZoDJfcxrUxE', 'CLIENTE', CURRENT_DATE, 0);

-- Itens de Pedido
insert into ItemPedido (panela_id, quantidade, valor_unitario, data_cadastro, version) values (1, 2, 24990.0, CURRENT_DATE, 0);
insert into ItemPedido (panela_id, quantidade, valor_unitario, data_cadastro, version) values (2, 1, 49990.0, CURRENT_DATE, 0);
insert into ItemPedido (panela_id, quantidade, valor_unitario, data_cadastro, version) values (3, 1, 32990.0, CURRENT_DATE, 0);
insert into ItemPedido (panela_id, quantidade, valor_unitario, data_cadastro, version) values (1, 3, 24990.0, CURRENT_DATE, 0);
insert into ItemPedido (panela_id, quantidade, valor_unitario, data_cadastro, version) values (2, 2, 49990.0, CURRENT_DATE, 0);
insert into ItemPedido (panela_id, quantidade, valor_unitario, data_cadastro, version) values (3, 1, 32990.0, CURRENT_DATE, 0);

-- Pedidos (status: 1 = AGUARDANDO_PAGAMENTO, 5 = ENTREGUE)
insert into Pedido (usuario_id, entrega_logradouro, entrega_numero, entrega_complemento, entrega_bairro, entrega_cidade, entrega_estado, entrega_cep, codigo_status_pedido, valor_itens, valor_desconto, valor_frete, data_cadastro, version) values (1, 'Rua das Flores', '123', 'Apto 101', 'Centro', 'São Paulo', 'SP', '01234-567', 1, 99970.00, 0.00, 0.00, CURRENT_DATE, 0);
insert into Pedido (usuario_id, entrega_logradouro, entrega_numero, entrega_complemento, entrega_bairro, entrega_cidade, entrega_estado, entrega_cep, codigo_status_pedido, valor_itens, valor_desconto, valor_frete, data_cadastro, version) values (2, 'Avenida Rio Branco', '156', null, 'Centro', 'Rio de Janeiro', 'RJ', '20040-020', 5, 32990.00, 0.00, 0.00, CURRENT_DATE, 0);
insert into Pedido (usuario_id, entrega_logradouro, entrega_numero, entrega_complemento, entrega_bairro, entrega_cidade, entrega_estado, entrega_cep, codigo_status_pedido, valor_itens, valor_desconto, valor_frete, data_cadastro, version) values (3, 'Avenida Getúlio Vargas', '1200', null, 'Savassi', 'Belo Horizonte', 'MG', '30130-100', 1, 207940.00, 0.00, 0.00, CURRENT_DATE, 0);

-- Associação Pedido <-> ItemPedido
insert into pedido_itempedido (Pedido_id, itens_id) values (1, 1);
insert into pedido_itempedido (Pedido_id, itens_id) values (1, 2);
insert into pedido_itempedido (Pedido_id, itens_id) values (2, 3);
insert into pedido_itempedido (Pedido_id, itens_id) values (3, 4);
insert into pedido_itempedido (Pedido_id, itens_id) values (3, 5);
insert into pedido_itempedido (Pedido_id, itens_id) values (3, 6);

-- Pagamentos (metodo: 4 = PIX, 1 = CARTAO_CREDITO; status: 2 = AGUARDANDO_PAGAMENTO, 4 = PAGO)
insert into Pagamento (pedido_id, codigo_metodo_pagamento, codigo_status_pagamento, valor, gateway, gateway_payment_id, idempotency_key, expira_em, pix_txid, pix_qr_code_copia_e_cola, data_cadastro, version) values (1, 4, 2, 99970.00, 'fake', 'fake_seed_pix_1', 'seed-pedido-1', CURRENT_TIMESTAMP + INTERVAL '1' DAY, 'seed-txid-1', '00020126...FAKEseed-txid-1', CURRENT_DATE, 0);
insert into Pagamento (pedido_id, codigo_metodo_pagamento, codigo_status_pagamento, valor, gateway, gateway_payment_id, idempotency_key, pago_em, cartao_bandeira, cartao_ultimos4_digitos, cartao_parcelas, cartao_codigo_autorizacao, data_cadastro, version) values (2, 1, 4, 32990.00, 'fake', 'fake_seed_cartao_2', 'seed-pedido-2', CURRENT_DATE, 'VISA', '4242', 1, 'SEED01', CURRENT_DATE, 0);
