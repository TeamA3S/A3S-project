package com.example.a3sproject.config;

import com.example.a3sproject.global.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toH2Console;
import static org.springframework.boot.security.autoconfigure.web.servlet.PathRequest.toStaticResources;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용 시 불필요)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정 (portone-sdk.js 허용)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Session 사용 안 함 (Stateless)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // H2 Console 허용
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))

            // 요청 권한 설정
            .authorizeHttpRequests(authorize -> authorize
                     // 1) 정적 리소스
                    .requestMatchers(toStaticResources().atCommonLocations()).permitAll()

                    // 2) 템플릿 페이지 렌더링
                    .requestMatchers(HttpMethod.GET, "/").permitAll()
                    .requestMatchers(HttpMethod.GET, "/pages/**").permitAll()

                    // 3) 공개 API
                    .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()

                    // 4) 인증 API
                    .requestMatchers(HttpMethod.POST,
                            "/api/auth/login",
                            "/api/users/signUp",
                            "/api/auth/reissue"
                    ).permitAll()
                    .requestMatchers("/api/payments/webhook").permitAll()
                    // 헬스체크 허용
                    .requestMatchers("/actuator/**").permitAll()
                    .requestMatchers("/h2-console/**").permitAll()
                    // H2 Console 허용
                    .requestMatchers(toH2Console()).permitAll()

                    // 5) 그 외 API는 인증 필요
                    .requestMatchers("/api/**").authenticated()

                    // 6) 나머지 전부 인증 필요
                    .anyRequest().authenticated()
            )

                // 인증/인가 실패 처리
                .exceptionHandling(ex -> ex
                        // 인증 정보가 없는 요청 (토큰 없음) → 401
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다.\",\"data\":null}"
                            );
                        })
                        // 인증은 됐지만 권한이 없는 요청 → 403
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"code\":\"FORBIDDEN\",\"message\":\"접근 권한이 없습니다.\",\"data\":null}"
                            );
                        })
                )

            // JWT 필터 추가
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * PasswordEncoder Bean
     */
    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:8080",
                "https://a3s.click"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
