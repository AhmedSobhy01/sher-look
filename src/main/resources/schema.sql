PRAGMA journal_mode=WAL;
PRAGMA busy_timeout = 5000;

CREATE TABLE IF NOT EXISTS documents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT UNIQUE,
    title TEXT,
    description TEXT,
    file_path TEXT NOT NULL,
    document_hash STRING NOT NULL,
    crawl_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    index_time DATETIME DEFAULT NULL,
    page_rank REAL DEFAULT 0.0 NOT NULL,
    document_size INTEGER DEFAULT 0 NOT NULL
);
CREATE TABLE IF NOT EXISTS words (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT NOT NULL UNIQUE,
    count INTEGER DEFAULT 0 NOT NULL,
    idf REAL DEFAULT 0.0 NOT NULL,
    stem TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS document_words (
    document_id INTEGER,
    word_id INTEGER,
    position INTEGER,
    section TEXT DEFAULT 'body',
    FOREIGN KEY(document_id) REFERENCES documents(id),
    FOREIGN KEY(word_id) REFERENCES words(id),
    PRIMARY KEY(document_id, word_id, position)
);

CREATE TABLE IF NOT EXISTS links (
    source_document_id INTEGER,
    target_url TEXT NOT NULL,
    FOREIGN KEY(source_document_id) REFERENCES documents(id),
    PRIMARY KEY(source_document_id, target_url)
);

CREATE VIRTUAL TABLE IF NOT EXISTS documents_fts USING fts5(
    content
);


CREATE INDEX IF NOT EXISTS idx_documents_wid_docid ON document_words(word_id, document_id);
CREATE INDEX IF NOT EXISTS idx_document_words ON document_words(word_id);
CREATE INDEX IF NOT EXISTS idx_documents_documents ON document_words(document_id);
