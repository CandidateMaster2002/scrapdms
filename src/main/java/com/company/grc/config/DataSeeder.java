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
                        .name("ScrapDMS")
                        .email("contcat@scrapdms.com")
                        .mobileNo("9560758420")
                        .password("password")
                        .role("SUPER_ADMIN")
                        .build();

                userRepository.save(admin);
                System.out.println("Default Super Admin created: contcat@scrapdms.com / password");
            }
        };
    }
}
