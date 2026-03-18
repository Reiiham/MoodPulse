package ma.reihaam.moodpulsegateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import ma.reihaam.moodpulsegateway.model.SpotifyTokenResponse;
import ma.reihaam.moodpulsegateway.service.SpotifyAuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

import static reactor.netty.http.HttpConnectionLiveness.log;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Spotify Auth", description = "Spotify OAuth 2.0 flow")
public class SpotifyAuthController {

    private final SpotifyAuthService spotifyAuthService;

    @Operation(summary = "Redirect user to Spotify login")
    @GetMapping("/login")
    public void login(
            HttpServletResponse response,
            @RequestParam(defaultValue = "medium_term") String period) throws IOException {
        String authUrl = spotifyAuthService.buildAuthorizationUrl(period);
        log.info("Redirecting to Spotify, period: {}", period);
        response.sendRedirect(authUrl);
    }

    @GetMapping("/callback")
    public void callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {

        if (error != null) {
            response.sendRedirect("https://mood-pulse-nine.vercel.app/index.html?error=" + error);
            return;
        }

        try {
            SpotifyTokenResponse token = spotifyAuthService.exchangeCode(code);

            // Extract period from state (format: "period_XXXX")
            // Extract period from state (format: "short_term|abc12345")
            String period = "medium_term";
            if (state != null && state.contains("|")) {
                period = state.substring(0, state.indexOf("|"));
            }

            log.info("Period extracted from state: {}", period);

            response.sendRedirect(
                    "https://mood-pulse-nine.vercel.app/index.html"
                            + "?access_token=" + token.getAccessToken()
                            + "&expires_in=" + token.getExpiresIn()
                            + "&period=" + period
            );

        } catch (Exception e) {
            log.error("Token exchange failed: {}", e.getMessage(), e);
            response.sendRedirect("https://mood-pulse-nine.vercel.app/index.html?error=token_failed");
        }
    }

    @Operation(summary = "Refresh an expired access token")
    @PostMapping("/refresh")
    public ResponseEntity<SpotifyTokenResponse> refresh(
            @RequestParam String refreshToken) {
        SpotifyTokenResponse token = spotifyAuthService.refreshToken(refreshToken);
        return ResponseEntity.ok(token);
    }
}