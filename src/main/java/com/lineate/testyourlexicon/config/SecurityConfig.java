package com.lineate.testyourlexicon.config;

import com.lineate.testyourlexicon.security.LogoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

@Configuration
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(authz -> authz
        .requestMatchers(HttpMethod.GET,
          "/users").hasAnyRole("ADMIN")
        .requestMatchers("/health").authenticated()
        .anyRequest().permitAll())
      .logout(logout -> logout
        .logoutSuccessHandler(new LogoutHandler()))
      .build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

}
