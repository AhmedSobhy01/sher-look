CREATE TABLE IF NOT EXISTS documents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    url TEXT UNIQUE,
    title TEXT,
    description TEXT,
    file_path TEXT NOT NULL,
    crawl_time DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    index_time DATETIME DEFAULT NULL
);
CREATE TABLE IF NOT EXISTS words (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    word TEXT NOT NULL UNIQUE
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