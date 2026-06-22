-- insere contas iniciais para testes de Depósito, Saque e Transferência
INSERT INTO ebanx_api.account_asset (account_id, amount, version) VALUES
('1234567890', 1500.50, 0),   -- Positivo
('987654321X', -250.00, 0),   -- Negativo com dígito X
('5554443332', 0.00, 0),      -- Zerado
('111222333X', 4200.75, 0),   -- Positivo com dígito X
('9998887776', -15.20, 0),    -- Negativo
('12345', 890.00, 0);         -- Conta curta (positivo)
