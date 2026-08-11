package com.afb.transferplatform.config;

import com.afb.transferplatform.repository.AgentRepository;
import com.afb.transferplatform.repository.TransfertRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seed(
            AgentRepository agents,
            TransfertRepository transferts,
            PasswordEncoder encoder
    ) {
        return args -> {
            // No demo data
        };
    }
}