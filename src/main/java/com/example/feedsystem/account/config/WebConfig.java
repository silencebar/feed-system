package com.example.feedsystem.account.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtAuthenticationInterceptor jwtAuthenticationInterceptor;

    @Value("${feedsystem.upload.root:.run/uploads}")
    private String uploadRoot;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtAuthenticationInterceptor)
                .addPathPatterns(
                        "/account/**",
                        "/social/**",
                        "/video/**",
                        "/like/**",
                        "/comment/**",
                        "/message/**",
                        "/creator-assistant/**",
                        "/feed/listByFollowing"
                )
                .excludePathPatterns(
                        "/account/register",
                        "/account/login",
                        "/account/refresh",
                        "/account/findByID",
                        "/account/findByUsername",
                        "/video/listByAuthorID",
                        "/video/getDetail",
                        "/comment/listAll"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("file:" + uploadRoot.replace("\\", "/") + "/");
    }
}
