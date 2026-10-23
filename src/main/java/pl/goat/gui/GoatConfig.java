package pl.goat.gui;

/**
 * Proste, statyczne przechowywanie ustawień GUI w pamięci (reset po restarcie gry).
 * Jeśli chcesz zapis do pliku (np. config/goatgui.json), dopisz to tutaj z Gson —
 * Fabric API zwykle ma Gson na classpath.
 */
public class GoatConfig {

    // ---- Motywy kolorystyczne (Visuals) ----
    public enum Theme {
        ROZOWY("Rozowy", 0xFFE05A8F, 0xFFC94B7D),
        NIEBIESKI("Niebieski", 0xFF3B82F6, 0xFF2563EB),
        ZIELONY("Zielony", 0xFF22C55E, 0xFF16A34A),
        FIOLETOWY("Fioletowy", 0xFF8B5CF6, 0xFF7C3AED);

        public final String label;
        public final int accent;
        public final int accentHover;

        Theme(String label, int accent, int accentHover) {
            this.label = label;
            this.accent = accent;
            this.accentHover = accentHover;
        }

        public Theme next() {
            Theme[] v = values();
            return v[(this.ordinal() + 1) % v.length];
        }
    }

    public static Theme theme = Theme.ROZOWY;

    // ---- Ustawienia (przeniesione tu logicznie spod Visuals) ----
    public static boolean showCoords = false;
    public static boolean showPing = true;
    public static boolean compactList = false;
    public static boolean sortByName = false;

    // ---- API sprawdzania wyciekow po nicku ----
    // Uzupelnij wlasnym endpointem i kluczem API dostawcy, ktorego wybierzesz.
    // BreachChecker wysyla GET na BREACH_API_URL + nick i oczekuje odpowiedzi JSON.
    public static String breachApiUrl = "https://api.example-breach-service.com/v1/check?name=";
    public static String breachApiKey = "";
}
