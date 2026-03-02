ALTER TABLE auction
  ADD FULLTEXT INDEX ft_auction_name_desc (name, description);

ALTER TABLE post
  ADD FULLTEXT INDEX ft_post_title_content (title, content);