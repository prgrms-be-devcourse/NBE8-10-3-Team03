CREATE TABLE search_document_mv (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_type VARCHAR(20) NOT NULL,
    source_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NULL,
    price INT NULL,
    status VARCHAR(30) NULL,
    category_id BIGINT NULL,
    thumbnail_url VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_search_document_mv_type_source (document_type, source_id),
    FULLTEXT KEY ft_search_document_mv_title_content (title, content),
    INDEX idx_search_document_mv_created_at_id (created_at DESC, id DESC),
    INDEX idx_search_document_mv_deleted (deleted),
    INDEX idx_search_document_mv_category_id (category_id),
    INDEX idx_search_document_mv_status (status)
);

CREATE TABLE search_document_mv_next LIKE search_document_mv;