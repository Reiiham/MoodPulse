package ma.reihaam.moodpulsegateway.service;

import ma.reihaam.moodpulsegateway.model.SpotifyTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Base64;
import java.util.UUID;

@Service
@Slf4j
public class SpotifyAuthService {

    @Value("${spotify.client-id}")
    private String clientId;

    @Value("${spotify.client-secret}")
    private String clientSecret;

    private static final String REDIRECT_URI = "https://moodpulse-production.up.railway.app/callback";
    private static final String TOKEN_URL     = "https://accounts.spotify.com/api/token";
    private static final String AUTH_URL      = "https://accounts.spotify.com/authorize";

    private final WebClient webClient = WebClient.builder().build();

    public String buildAuthorizationUrl(String period) {
        String randomPart = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        // Use | as separator instead of _ to avoid cutting off "short_term"
        String state = period + "|" + randomPart;

        return AUTH_URL
                + "?response_type=code"
                + "&client_id=" + clientId.trim()
                + "&scope=user-top-read%20user-read-private"
                + "&redirect_uri=https%3A%2F%2Fmoodpulse-production.up.railway.app%2Fcallback"
                + "&state=" + state
                + "&show_dialog=false";
    }
    public SpotifyTokenResponse exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",    "authorization_code");
        form.add("code",          code.trim());
        form.add("redirect_uri",  REDIRECT_URI);

        String auth = basicAuth();
        log.info("Exchanging code, redirect_uri={}", REDIRECT_URI);
        log.info("Basic auth header present: {}", auth != null);

        try {
            SpotifyTokenResponse response = webClient.post()
                    .uri(TOKEN_URL)
                    .header(HttpHeaders.AUTHORIZATION, auth)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(SpotifyTokenResponse.class)
                    .block();

            log.info("Token exchange successful!");
            return response;

        } catch (WebClientResponseException e) {
            log.error("Spotify token error: {} — body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }

    public SpotifyTokenResponse refreshToken(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type",    "refresh_token");
        form.add("refresh_token", refreshToken);

        return webClient.post()
                .uri(TOKEN_URL)
                .header(HttpHeaders.AUTHORIZATION, basicAuth())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(SpotifyTokenResponse.class)
                .block();
    }

    private String basicAuth() {
        String credentials = clientId.trim() + ":" + clientSecret.trim();
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());
    }
}