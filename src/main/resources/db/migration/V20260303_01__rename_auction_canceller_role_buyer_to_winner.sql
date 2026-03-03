UPDATE auction
SET canceller_role = 'WINNER'
WHERE canceller_role = 'BUYER';
