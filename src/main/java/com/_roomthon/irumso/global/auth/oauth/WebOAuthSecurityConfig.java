package com._roomthon.irumso.global.auth.oauth;

import com._roomthon.irumso.global.auth.jwt.TokenProvider;
import com._roomthon.irumso.refreshToken.RefreshTokenRepository;
import com._roomthon.irumso.user.UserRepository;
import com._roomthon.irumso.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@RequiredArgsConstructor
@Configuration
public class WebOAuthSecurityConfig {
    private final OAuth2UserCustomService oAuth2UserCustomService;
    private final TokenProvider tokenProvider;
    private final UserService userService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.csrf().disable()
                .httpBasic().disable()
                .formLogin().disable()
                .logout().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.addFilterBefore(tokenAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        http.cors();

        http.authorizeRequests()
                .requestMatchers("/api/target-audience/**", "/api/rank/**","/api/youth-policy/fetch-and-save").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll();

        http.oauth2Login()
                .authorizationEndpoint()
                .authorizationRequestRepository(oAuth2AuthorizationRequestBasedOnCookieRepository())
                .and()
                .successHandler(oAuth2SuccessHandler())
                .userInfoEndpoint()
                .userService(oAuth2UserCustomService);

        http.logout()
                .logoutUrl("/logout")  // 로그아웃 URL 설정
                .logoutSuccessUrl("/")  // 로그아웃 성공 후 리디렉션할 URL 설정
                .invalidateHttpSession(true)  // 세션 무효화
                .deleteCookies("JSESSIONID")  // 쿠키 삭제
                .permitAll();  // 로그아웃 URL은 모두 접근 가능하게 설정

        http.exceptionHandling()
                .defaultAuthenticationEntryPointFor(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                        new AntPathRequestMatcher("/api/**"));

        return http.build();
    }

    @Bean
    public TokenAuthenticationFilter tokenAuthenticationFilter() {
        return new TokenAuthenticationFilter(tokenProvider);
    }
    @Bean
    public OAuth2AuthorizationRequestBasedOnCookieRepository oAuth2AuthorizationRequestBasedOnCookieRepository() {
        return new OAuth2AuthorizationRequestBasedOnCookieRepository();
    }
    @Bean
    public OAuth2SuccessHandler oAuth2SuccessHandler() {
        return new OAuth2SuccessHandler(tokenProvider,
                refreshTokenRepository,
                userRepository,
                oAuth2AuthorizationRequestBasedOnCookieRepository(),
                userService
        );
    }

}