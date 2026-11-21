package com.redsaludandina;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Monolito modular de teleconsulta rural de RedSalud Andina. Un paquete por contexto delimitado del
 * caso: identity, scheduling, episode, prescription, referral, legacy e insights.
 */
@SpringBootApplication
public class RedSaludAndinaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedSaludAndinaApplication.class, args);
    }
}
