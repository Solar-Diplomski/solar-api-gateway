package fer.solar.solar_api_gateway.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;

    @Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
    private String audience;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .cors(corsSpec -> corsSpec.configurationSource(corsConfigurationSource()))
            .authorizeExchange(authorizeExchangeSpec ->
                authorizeExchangeSpec
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/power_plant/**").permitAll()
                    .pathMatchers("/models/**").permitAll()
                    .pathMatchers("/forecast/**").permitAll()
                    .pathMatchers("/reading/**").permitAll()
                    .pathMatchers("/features/**").permitAll()
                    .pathMatchers("/metric/**").permitAll()
                    .pathMatchers("/playground/**").permitAll()
                    .pathMatchers("/generate/**").permitAll()
                    .pathMatchers(HttpMethod.POST, "/users/**").hasAuthority("PERMISSION_user:create")
                    .pathMatchers(HttpMethod.GET, "/users/**").hasAuthority("PERMISSION_user:read")
                    .pathMatchers(HttpMethod.PUT, "/users/{userId}").hasAuthority("PERMISSION_user:update")
                    .pathMatchers(HttpMethod.DELETE, "/users/**").hasAuthority("PERMISSION_user:delete")
                    .pathMatchers(HttpMethod.POST, "/roles/**").hasAuthority("PERMISSION_role:create")
                    .pathMatchers(HttpMethod.GET, "/roles/**").hasAuthority("PERMISSION_role:read")
                    .pathMatchers(HttpMethod.PUT, "/roles/**").hasAuthority("PERMISSION_role:update")
                    .pathMatchers(HttpMethod.DELETE, "/roles/**").hasAuthority("PERMISSION_role:delete")
                    .pathMatchers(HttpMethod.GET, "/permissions/**").hasAuthority("PERMISSION_permission:read")
                    .pathMatchers(HttpMethod.PUT, "/permissions/**").hasAuthority("PERMISSION_permission:update")
                    .anyExchange().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
                jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor());
                jwt.jwtDecoder(jwtDecoder());
            }));
        return http.build();
    }
    
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("http://localhost:5173"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    ReactiveJwtDecoder jwtDecoder() {
        NimbusReactiveJwtDecoder jwtDecoder = NimbusReactiveJwtDecoder.withIssuerLocation(issuer).build();

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(audience);
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> combinedTokenValidator = new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator);

        jwtDecoder.setJwtValidator(combinedTokenValidator);

        return jwtDecoder;
    }

    @Bean
    public ReactiveJwtAuthenticationConverter grantedAuthoritiesExtractor() {
        ReactiveJwtAuthenticationConverter jwtAuthenticationConverter = new ReactiveJwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(new JwtScopeConverter());
        return jwtAuthenticationConverter;
    }
} 