INSERT INTO accounts(user_id, balance)
VALUES
    (1, 1000.00),
    (2, 500.00),
    (3, 300.00)
ON CONFLICT (user_id) DO NOTHING;
