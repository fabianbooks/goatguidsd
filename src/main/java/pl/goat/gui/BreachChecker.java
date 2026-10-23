package pl.goat.gui;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sprawdza, czy dany nick pojawial sie w publicznie znanych wyciekach danych,
 * poprzez zapytanie do zewnetrznego API (patrz GoatConfig.breachApiUrl/breachApiKey).
 *
 * UWAGA: ten kod nie byl przetestowany w tym srodowisku (brak dostepu do sieci
 * w sandboxie), dopasuj parsowanie odpowiedzi (parseResult) do faktycznego
 * formatu JSON serwisu, ktorego uzyjesz.
 */
public class BreachChecker {

    public enum Status { LOADING, FOUND, CLEAN, ERROR }

    public static class Result {
        public final Status status;
        public final String detail;

        public Result(Status status, String detail) {
            this.status = status;
            this.detail = detail;
        }
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // Cache wynikow na sesje gry, zeby nie spamowac API przy kazdym otwarciu GUI
    private static final Map<String, Result> CACHE = new ConcurrentHashMap<>();

    public static Result cached(String nick) {
        return CACHE.get(nick.toLowerCase());
    }

    /**
     * Asynchroniczne zapytanie. Wynik laduje w cache, GoatScreen odczytuje go
     * przy kolejnym rysowaniu klatki (polling cache w drawPlayers/drawToggles).
     */
    public static void checkAsync(String nick) {
        String key = nick.toLowerCase();
        CACHE.put(key, new Result(Status.LOADING, null));

        if (GoatConfig.breachApiUrl == null || GoatConfig.breachApiUrl.isBlank()
                || GoatConfig.breachApiKey == null || GoatConfig.breachApiKey.isBlank()) {
            CACHE.put(key, new Result(Status.ERROR, "Brak skonfigurowanego API"));
            return;
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GoatConfig.breachApiUrl + nick))
                .header("Authorization", "Bearer " + GoatConfig.breachApiKey)
                .timeout(Duration.ofSeconds(8))
                .GET()
                .build();

        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> CACHE.put(key, parseResult(resp.statusCode(), resp.body())))
                .exceptionally(ex -> {
                    CACHE.put(key, new Result(Status.ERROR, "Blad zapytania: " + ex.getMessage()));
                    return null;
                });
    }

    /**
     * Dopasuj to do formatu odpowiedzi swojego dostawcy API.
     * Domyslnie oczekuje czegos w stylu: {"found": true/false, "sources": N}
     */
    private static Result parseResult(int statusCode, String body) {
        if (statusCode != 200) {
            return new Result(Status.ERROR, "HTTP " + statusCode);
        }
        try {
            boolean found = body.contains("\"found\":true") || body.contains("\"found\": true");
            if (found) {
                return new Result(Status.FOUND, "Nick wystepuje w znanych wyciekach");
            }
            return new Result(Status.CLEAN, "Brak w znanych wyciekach");
        } catch (Exception e) {
            return new Result(Status.ERROR, "Nieprawidlowa odpowiedz API");
        }
    }
}
