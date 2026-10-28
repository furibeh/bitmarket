package com.bitmarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.modulith.Modulithic;

@SpringBootApplication
@Modulithic(sharedModules = "shared")
public class BitmarketApplication {

  public static void main(String[] args) {
    SpringApplication.run(BitmarketApplication.class, args);
  }
}
