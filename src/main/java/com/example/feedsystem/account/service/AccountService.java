package com.example.feedsystem.account.service;

import com.example.feedsystem.account.dto.AccountVO;
import com.example.feedsystem.account.dto.ChangePasswordRequest;
import com.example.feedsystem.account.dto.LoginRequest;
import com.example.feedsystem.account.dto.LoginResponse;
import com.example.feedsystem.account.dto.RefreshRequest;
import com.example.feedsystem.account.dto.RefreshResponse;
import com.example.feedsystem.account.dto.RegisterRequest;
import com.example.feedsystem.account.dto.RenameRequest;
import com.example.feedsystem.account.dto.UpdateProfileRequest;
import com.example.feedsystem.account.mapper.AccountMapper;
import com.example.feedsystem.account.model.AccountDO;
import com.example.feedsystem.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AccountTokenCache accountTokenCache;

    @Transactional
    public void register(RegisterRequest request) {
        AccountDO account = new AccountDO();
        account.setUsername(request.getUsername());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        try {
            accountMapper.insert(account);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "username already exists");
        }
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        AccountDO account = accountMapper.selectByUsername(request.getUsername());
        if (account == null || !passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "invalid username or password");
        }

        String oldRefreshToken = account.getRefreshToken();
        String accessToken = jwtService.generateAccessToken(account.getId(), account.getUsername());
        String refreshToken = refreshTokenService.generateRefreshToken();
        accountMapper.updateLoginTokens(account.getId(), accessToken, refreshToken);

        if (oldRefreshToken != null && !oldRefreshToken.isBlank()) {
            accountTokenCache.deleteTokens(account.getId(), oldRefreshToken);
        }
        accountTokenCache.saveLoginTokens(account.getId(), accessToken, refreshToken);
        return new LoginResponse(accessToken, refreshToken, account.getId(), account.getUsername());
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        Long accountId = accountTokenCache.getAccountIdByRefreshToken(refreshToken);
        AccountDO account = accountId == null
                ? accountMapper.selectByRefreshToken(refreshToken)
                : accountMapper.selectById(accountId);

        if (account == null || !refreshToken.equals(account.getRefreshToken())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "invalid refresh token");
        }

        String accessToken = jwtService.generateAccessToken(account.getId(), account.getUsername());
        accountMapper.updateToken(account.getId(), accessToken);
        accountTokenCache.saveAccessToken(account.getId(), accessToken);
        return new RefreshResponse(accessToken, account.getId(), account.getUsername());
    }

    @Transactional
    public void logout(Long accountId) {
        AccountDO account = requireAccount(accountId);
        accountMapper.clearTokens(accountId);
        accountTokenCache.deleteTokens(accountId, account.getRefreshToken());
    }

    @Transactional
    public LoginResponse rename(Long accountId, RenameRequest request) {
        AccountDO account = requireAccount(accountId);
        String accessToken = jwtService.generateAccessToken(accountId, request.getUsername());
        try {
            accountMapper.updateUsernameAndToken(accountId, request.getUsername(), accessToken);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(HttpStatus.CONFLICT, "username already exists");
        }
        accountTokenCache.saveAccessToken(accountId, accessToken);
        return new LoginResponse(accessToken, account.getRefreshToken(), accountId, request.getUsername());
    }

    @Transactional
    public void changePassword(Long accountId, ChangePasswordRequest request) {
        AccountDO account = requireAccount(accountId);
        if (!passwordEncoder.matches(request.getOldPassword(), account.getPassword())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "invalid password");
        }
        accountMapper.updatePassword(accountId, passwordEncoder.encode(request.getNewPassword()));
        accountMapper.clearTokens(accountId);
        accountTokenCache.deleteTokens(accountId, account.getRefreshToken());
    }

    @Transactional
    public AccountVO updateProfile(Long accountId, UpdateProfileRequest request) {
        requireAccount(accountId);
        accountMapper.updateProfile(accountId, request.getAvatarUrl(), request.getBio());
        return AccountVO.from(requireAccount(accountId));
    }

    public AccountVO profile(Long accountId) {
        return AccountVO.from(requireAccount(accountId));
    }

    public AccountVO findById(Long accountId) {
        return AccountVO.from(requireAccount(accountId));
    }

    public AccountVO findByUsername(String username) {
        AccountDO account = accountMapper.selectByUsername(username);
        if (account == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "account not found");
        }
        return AccountVO.from(account);
    }

    private AccountDO requireAccount(Long accountId) {
        AccountDO account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(HttpStatus.NOT_FOUND, "account not found");
        }
        return account;
    }
}
