package com.sherlook.crawler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Crawler {
    @Value("${crawler.threads}")
    private int threads;

    @Value("${crawler.max-pages}")
    private int maxPages;

    public void start() {
        System.out.println("Starting crawler with " + threads + " threads");
    }
}