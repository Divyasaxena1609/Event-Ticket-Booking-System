package com.ticketbooking.auth_service.service;

import com.ticketbooking.auth_service.config.JwtProperties;
import com.ticketbooking.auth_service.config.PasswordResetProperties;
import com.ticketbooking.auth_service.dto.payload.ForgotPasswordRequest;
import com.ticketbooking.auth_service.dto.payload.LoginRequest;
import com.ticketbooking.auth_service.dto.payload.RefreshTokenRequest;
import com.ticketbooking.auth_service.dto.payload.RegisterRequest;
import com.ticketbooking.auth_service.dto.payload.ResetPasswordRequest;
import com.ticketbooking.auth_service.dto.response.TokenResponse;
import com.ticketbooking.auth_service.dto.response.TokenValidationResponse;
import com.ticketbooking.auth_service.entity.AuthAccount;
import com.ticketbooking.auth_service.exception.AuthException;
import com.ticketbooking.auth_service.repository.AuthAccountRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.client.RestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthService {
    private final AuthAccountRepository accountRepository;
    private final JwtProperties jwtProperties;
    private final PasswordResetProperties passwordResetProperties;
    private final JavaMailSender mailSender;
    private final RestClient userServiceClient;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(AuthAccountRepository accountRepository, JwtProperties jwtProperties,
                       PasswordResetProperties passwordResetProperties, JavaMailSender mailSender,
                       @Value("${services.user.url:http://localhost:8085}") String userServiceUrl) {
        this.accountRepository = accountRepository;
        this.jwtProperties = jwtProperties;
        this.passwordResetProperties = passwordResetProperties;
        this.mailSender = mailSender;
        this.userServiceClient = RestClient.builder().baseUrl(userServiceUrl).build();
    }

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
        linkProfileIfNeeded(account);
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

    /** Always succeeds so this endpoint does not reveal which email addresses have accounts. */
    public void requestPasswordReset(ForgotPasswordRequest request) {
        AuthAccount account = accountRepository.findByEmailIgnoreCase(request.getEmail()).orElseGet(() -> createAuthAccountForExistingProfile(request.getEmail()));
        if (account != null) {
            linkProfileIfNeeded(account);
            String token = UUID.randomUUID() + "." + UUID.randomUUID();
            account.setPasswordResetTokenHash(hash(token));
            account.setPasswordResetTokenExpiresAt(OffsetDateTime.now().plus(passwordResetProperties.getExpiration()));
            accountRepository.save(account);
            sendPasswordResetEmail(account.getEmail(), token);
        }
    }

    public void resetPassword(ResetPasswordRequest request) {
        String tokenHash = hash(request.getToken());
        AuthAccount account = accountRepository.findByPasswordResetTokenHash(tokenHash)
                .orElseThrow(() -> new AuthException(HttpStatus.BAD_REQUEST, "This password reset link is invalid or has expired"));
        if (account.getPasswordResetTokenExpiresAt() == null || account.getPasswordResetTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            account.setPasswordResetTokenHash(null);
            account.setPasswordResetTokenExpiresAt(null);
            accountRepository.save(account);
            throw new AuthException(HttpStatus.BAD_REQUEST, "This password reset link is invalid or has expired");
        }
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setPasswordResetTokenHash(null);
        account.setPasswordResetTokenExpiresAt(null);
        // Invalidate every existing browser session after a password change.
        account.setRefreshTokenHash(null);
        account.setRefreshTokenExpiresAt(null);
        accountRepository.save(account);
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
            if (!expectedType.equals(claims.get("type", String.class))) {
                throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid token type: expected " + expectedType + " token");
            }
            return claims;
        } catch (AuthException exception) { throw exception; }
        catch (Exception exception) { throw new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"); }
    }

    private SecretKey key() { return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret())); }
    private AuthAccount createAuthAccountForExistingProfile(String email) {
        String userUuid = lookupUserUuid(email);
        if (userUuid == null) return null;
        // Existing user profiles created before auth-service can now set their first password via email.
        return accountRepository.save(AuthAccount.builder().email(email).userUuid(userUuid)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())).active(true).build());
    }
    private void linkProfileIfNeeded(AuthAccount account) {
        if (account.getUserUuid() != null && !account.getUserUuid().isBlank()) return;
        String userUuid = lookupUserUuid(account.getEmail());
        if (userUuid == null) throw new AuthException(HttpStatus.UNAUTHORIZED, "Your account profile could not be found. Please contact support.");
        account.setUserUuid(userUuid);
        accountRepository.save(account);
    }
    private String lookupUserUuid(String email) {
        try {
            JsonNode response = userServiceClient.get()
                    .uri("/users/email/{email}", email)
                    .retrieve().body(JsonNode.class);
            String userUuid = response == null ? null : response.path("data").path("userUuid").asText(null);
            return userUuid == null || userUuid.isBlank() ? null : userUuid;
        } catch (Exception ignored) {
            return null;
        }
    }
    private void sendPasswordResetEmail(String email, String token) {
        String baseUrl = passwordResetProperties.getFrontendUrl().replaceAll("/+$", "");
        String link = baseUrl + "/reset-password?token=" + java.net.URLEncoder.encode(token, StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        if (passwordResetProperties.getFrom() != null && !passwordResetProperties.getFrom().isBlank()) message.setFrom(passwordResetProperties.getFrom());
        message.setTo(email);
        message.setSubject("Reset your EventHorizon password");
        message.setText("We received a request to reset your EventHorizon password.\n\nUse this one-time link within "
                + passwordResetProperties.getExpiration().toMinutes() + " minutes:\n" + link
                + "\n\nIf you did not request this, you can safely ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new AuthException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email delivery is not configured. Set SMTP_USERNAME, SMTP_PASSWORD, and MAIL_FROM in auth-service/.env, then restart auth-service.");
        }
    }
    private String hash(String value) { try { return java.util.Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
    private AuthException invalidCredentials() { return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid email or password"); }
    private AuthException invalidRefreshToken() { return new AuthException(HttpStatus.UNAUTHORIZED, "Invalid or expired refresh token"); }
}
