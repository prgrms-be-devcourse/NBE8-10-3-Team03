ALTER TABLE auction
  ADD INDEX idx_auction_status_create_id (status, create_date, id);

ALTER TABLE auction
  ADD INDEX idx_auction_category_create_id (category_id, create_date, id);

ALTER TABLE auction
  ADD INDEX idx_auction_category_status_create_id (category_id, status, create_date, id);
