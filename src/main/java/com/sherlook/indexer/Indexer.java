package com.sherlook.indexer;

import org.springframework.stereotype.Component;

@Component
public class Indexer {
    public void index() {
        System.out.println("Indexing documents");
    }
}