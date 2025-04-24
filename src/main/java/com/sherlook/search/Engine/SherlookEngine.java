package com.sherlook.search.Engine;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SherlookEngine {

  public static void main(String[] args) {
    SpringApplication.run(SherlookEngine.class, args);
  }

  @Bean
  public CommandLineRunner commandLineRunner(SherlookEngineManager manager) {
    return args -> {
      if (args.length > 0 && args[0].equals("offline")) {
        // manager.runOfflineProcessing();
      } else {
        // manager.startQueryServer();
      }
    };
  }
}
