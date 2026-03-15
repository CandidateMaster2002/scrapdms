package com.company.grc.config;

import com.company.grc.entity.UserEntity;
import com.company.grc.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    public CommandLineRunner initDatabase(UserRepository userRepository) {
        return args -> {
            if (userRepository.count() == 0) {
                UserEntity admin = UserEntity.builder()
                        .name("ScrapDMS Admin")
                        .email("admin@scrapdms.com")
                        .mobileNo("9999999999")
                        .password("ScrapDMS")
                        .role("super_admin")
                        .build();

                userRepository.save(admin);
                System.out.println("Default Super Admin created: admin@scrapdms.com / ScrapDMS");
            }
        };
    }
}
