package com.example.feedsystem.account.controller;

import com.example.feedsystem.account.dto.AccountVO;
import com.example.feedsystem.account.dto.ChangePasswordRequest;
import com.example.feedsystem.account.dto.FindAccountByIdRequest;
import com.example.feedsystem.account.dto.FindAccountByUsernameRequest;
import com.example.feedsystem.account.dto.LoginRequest;
import com.example.feedsystem.account.dto.LoginResponse;
import com.example.feedsystem.account.dto.MessageResponse;
import com.example.feedsystem.account.dto.RefreshRequest;
import com.example.feedsystem.account.dto.RefreshResponse;
import com.example.feedsystem.account.dto.RegisterRequest;
import com.example.feedsystem.account.dto.RenameRequest;
import com.example.feedsystem.account.dto.UpdateProfileRequest;
import com.example.feedsystem.account.service.AccountService;
import com.example.feedsystem.account.service.AuthenticatedAccount;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/register")
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        accountService.register(request);
        return new MessageResponse("account created");
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return accountService.login(request);
    }

    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return accountService.refresh(request);
    }

    @PostMapping("/findByID")
    public AccountVO findById(@Valid @RequestBody FindAccountByIdRequest request) {
        return accountService.findById(request.getId());
    }

    @PostMapping("/findByUsername")
    public AccountVO findByUsername(@Valid @RequestBody FindAccountByUsernameRequest request) {
        return accountService.findByUsername(request.getUsername());
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestAttribute("account") AuthenticatedAccount account) {
        accountService.logout(account.accountId());
        return new MessageResponse("logged out");
    }

    @PostMapping("/rename")
    public LoginResponse rename(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody RenameRequest request
    ) {
        return accountService.rename(account.accountId(), request);
    }

    @PatchMapping("/password")
    public MessageResponse changePassword(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        accountService.changePassword(account.accountId(), request);
        return new MessageResponse("password updated");
    }

    @PatchMapping("/updateProfile")
    public AccountVO updateProfile(
            @RequestAttribute("account") AuthenticatedAccount account,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        return accountService.updateProfile(account.accountId(), request);
    }

    @GetMapping("/profile")
    public AccountVO profile(@RequestAttribute("account") AuthenticatedAccount account) {
        return accountService.profile(account.accountId());
    }
}
