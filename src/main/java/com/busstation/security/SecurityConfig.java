package com.busstation.security;

import com.busstation.config.AppProperties;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserDetailsServiceImpl userDetailsService;
    private final AppProperties appProperties;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProvider())
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/routes/**", "/api/vehicles/**", "/api/trips/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/tickets").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/tickets/{id}/pay").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/tickets/me", "/api/tickets/search").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/routes").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/routes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/routes/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/vehicles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/vehicles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/vehicles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/trips").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/trips/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/trips/**").hasRole("ADMIN")
                        .requestMatchers("/api/drivers/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/**").authenticated()
                        .anyRequest().authenticated())
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "BUS_STATION_SESSION")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(204)));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(appProperties.getCors().getAllowedOrigins());
        configuration.setAllowedMethods(appProperties.getCors().getAllowedMethods());
        configuration.setAllowedHeaders(appProperties.getCors().getAllowedHeaders());
        configuration.setAllowCredentials(appProperties.getCors().isAllowCredentials());
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
