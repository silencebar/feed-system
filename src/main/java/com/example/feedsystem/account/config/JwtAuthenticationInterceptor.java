package com.example.feedsystem.account.config;

import com.example.feedsystem.account.mapper.AccountMapper;
import com.example.feedsystem.account.model.AccountDO;
import com.example.feedsystem.account.service.AccountTokenCache;
import com.example.feedsystem.account.service.AuthenticatedAccount;
import com.example.feedsystem.account.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtService jwtService;
    private final AccountTokenCache accountTokenCache;
    private final AccountMapper accountMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        String token = authorization.substring("Bearer ".length());
        Claims claims;
        try {
            claims = jwtService.parseAccessToken(token);
        } catch (JwtException | IllegalArgumentException ex) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        Number accountIdClaim = claims.get("account_id", Number.class);
        if (accountIdClaim == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
        Long accountId = accountIdClaim.longValue();
        String username = claims.get("username", String.class);

        String cachedToken = accountTokenCache.getAccessToken(accountId);
        if (!token.equals(cachedToken)) {
            AccountDO account = accountMapper.selectById(accountId);
            if (account == null || !token.equals(account.getToken())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }
            accountTokenCache.saveAccessToken(accountId, token);
        }

        request.setAttribute("account", new AuthenticatedAccount(accountId, username));
        return true;
    }
}
