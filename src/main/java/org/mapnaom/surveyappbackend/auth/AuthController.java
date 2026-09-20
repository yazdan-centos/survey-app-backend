package org.mapnaom.surveyappbackend.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.mapnaom.surveyappbackend.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        var accessToken = jwtService.issue(authentication);
        var claims = jwtService.parse(accessToken);
        var authorities = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toList();
        var roles = authorities.stream()
                .map(authority -> authority.startsWith("ROLE_") ? authority.substring("ROLE_".length()) : authority)
                .toList();

        return ResponseEntity.ok(new LoginResponse(
                accessToken,
                new UserResponse(
                        authentication.getName(),
                        authorities,
                        roles,
                        authorities.contains("ROLE_ADMIN") || roles.contains("ADMIN"),
                        claims.getExpiration().getTime())));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record LoginResponse(String accessToken, UserResponse user) {
    }

    public record UserResponse(String username, List<String> authorities, List<String> roles,
                               boolean isAdmin, long expiresAt) {
    }
}
