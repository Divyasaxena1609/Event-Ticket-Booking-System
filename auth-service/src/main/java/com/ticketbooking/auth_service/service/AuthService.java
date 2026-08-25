package com.ticketbooking.auth_service.service;

import com.ticketbooking.auth_service.config.JwtProperties;
import com.ticketbooking.auth_service.dto.payload.LoginRequest;
import com.ticketbooking.auth_service.dto.payload.RefreshTokenRequest;
import com.ticketbooking.auth_service.dto.payload.RegisterRequest;
import com.ticketbooking.auth_service.dto.response.TokenResponse;
import com.ticketbooking.auth_service.dto.response.TokenValidationResponse;
import com.ticketbooking.auth_service.entity.AuthAccount;
import com.ticketbooking.auth_service.exception.AuthException;
import com.ticketbooking.auth_service.repository.AuthAccountRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthAccountRepository accountRepository;
    private final JwtProperties jwtProperties;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public TokenResponse register(RegisterRequest request) {
        if (accountRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new AuthException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        AuthAccount account = AuthAccount.builder().email(request.getEmail()).passwordHash(passwordEncoder.encode(request.getPassword()))
                .userUuid(request.getUserUuid()).active(true).build();
        return issueTokens(accountRepository.save(account));
    }

    public TokenResponse login(LoginRequest request) {
        AuthAccount account = accountRepository.findByEmailIgnoreCase(request.getEmail())
                .orElseThrow(() -> invalidCredentials());
        if (!account.isActive() || !passwordEncoder.matches(request.getPassword(), account.getPasswordHash())) throw invalidCredentials();
        return issueTokens(account);
    }

    public TokenResponse refresh(RefreshTokenRequest request) {
        Claims claims = claims(request.getRefreshToken(), "refresh");
        AuthAccount account = accountRepository.findById(UUID.fromString(claims.getSubject()))
                .orElseThrow(() -> invalidRefreshToken());
        if (!account.isActive() || account.getRefreshTokenHash() == null || account.getRefreshTokenExpiresAt() == null
                || account.getRefreshTokenExpiresAt().isBefore(OffsetDateTime.now())
                || !MessageDigest.isEqual(account.getRefreshTokenHash().getBytes(StandardCharsets.UTF_8), hash(request.getRefreshToken()).getBytes(StandardCharsets.UTF_8))) {
            throw invalidRefreshToken();
        }
        return issueTokens(account);
    }

    public void logout(RefreshTokenRequest request) {
        Claims claims = claims(request.getRefreshToken(), "refresh");
        accountRepository.findById(UUID.fromString(claims.getSubject())).ifPresent(account -> {
            account.setRefreshTokenHash(null);
            account.setRefreshTokenExpiresAt(null);
            accountRepository.save(account);
        });
    }

    public TokenValidationResponse validate(String token) {
        Claims claims = claims(token, "access");
        return TokenValidationResponse.builder().valid(true).subject(claims.getSubject()).email(claims.get("email", String.class))
                .userUuid(claims.get("userUuid", String.class)).build();
    }

    private TokenResponse issueTokens(AuthAccount account) {
        OffsetDateTime now = OffsetDateTime.now();
        String accessToken = createToken(account, "access", now.plus(jwtProperties.getAccessTokenExpiration()));
        String refreshToken = createToken(account, "refresh", now.plus(jwtProperties.getRefreshTokenExpiration()));
        account.setRefreshTokenHash(hash(refreshToken));
        account.setRefreshTokenExpiresAt(now.plus(jwtProperties.getRefreshTokenExpiration()));
        accountRepository.save(account);
        return TokenResponse.builder().accessToken(accessToken).refreshToken(refreshToken).tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessTokenExpiration().toSeconds()).userUuid(account.getUserUuid()).build();
    }

    private String createToken(AuthAccount account, String type, OffsetDateTime expiresAt) {
        var builder = Jwts.builder().subject(account.getId().toString()).claim("type", type).claim("email", account.getEmail());
        if (account.getUserUuid() != null) builder.claim("userUuid", account.getUserUuid());
        return builder.issuedAt(new Date()).expiration(Date.from(expiresAt.toInstant())).signWith(key()).compact();
    }

    private Claims claims(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).getPayload();
            if (!expectedType.equals(claims.get("type", String.class))) throw invalidRefreshToken();
            return claims;
        } catch (AuthException exception) { throw exception; }
        catch (Exception exception) { throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"); }
    }

    private SecretKey key() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret())); }
    private String hash(String value) { try { return java.util.Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private AuthException invalidCredentials() { return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password"); }
    private AuthException invalidRefreshToken() { return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"); }
}
