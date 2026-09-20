package com.uphead.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

// UserDetailsServiceAutoConfiguration is excluded: auth is entirely
// JWT-based via a custom SecurityFilterChain, there's no UserDetailsService
// / form-login in play, so the default in-memory user it would generate is
// unused noise.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@ConfigurationPropertiesScan
public class InventoryPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryPlatformApplication.class, args);
	}

}
