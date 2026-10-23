package pl.goat.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GoatGUI - glowny ekran.
 *
 * Trzy zakladki (dol): Gracz / Visuals / Ustawienia.
 *  - Gracz:      lista graczy online + przycisk "Search" (sprawdza nick w bazie wyciekow)
 *  - Visuals:    wybor motywu kolorystycznego GUI
 *  - Ustawienia: przelaczniki (ping, koordynaty, kompaktowa lista, sortowanie po nicku)
 *
 * Uwaga: rysowanie glowek skorek graczy zostalo uproszczone do kolorowych
 * placeholderow, zeby uniknac zaleznosci od konkretnej wersji API tekstur
 * skina (to latwo przywrocic, patrz komentarz w drawPlayers()).
 */
public class GoatScreen extends Screen {

    private enum Tab {
        GRACZ("Gracz"),
        VISUALS("Visuals"),
        USTAWIENIA("Ustawienia");

        final String label;
        Tab(String label) { this.label = label; }
    }

    // ---- Wymiary panelu ----
    private static final int PANEL_W = 520;
    private static final int PANEL_H = 420;
    private static final int HEADER_H = 46;
    private static final int FOOTER_H = 40;
    private static final int CARD_W = 232;
    private static final int CARD_H = 44;
    private static final int GAP = 10;

    private Tab tab = Tab.GRACZ;
    private int scroll = 0;
    private String searchedPlayer = null;

    public GoatScreen() {
        super(Text.literal("GoatGUI"));
    }

    private int px() { return (this.width - PANEL_W) / 2; }
    private int py() { return (this.height - PANEL_H) / 2; }

    private List<PlayerListEntry> players() {
        List<PlayerListEntry> list = new ArrayList<>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            list.addAll(client.getNetworkHandler().getPlayerList());
        }
        if (GoatConfig.sortByName) {
            list.sort(Comparator.comparing(e -> e.getProfile().getName()));
        }
        return list;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);

        int x = px(), y = py();
        int accent = GoatConfig.theme.accent;

        // dim tlo
        ctx.fill(0, 0, this.width, this.height, 0x99000000);
        // panel
        ctx.fill(x, y, x + PANEL_W, y + PANEL_H, 0xF0101014);
        ctx.fill(x, y, x + PANEL_W, y + 1, 0x33FFFFFF);

        drawHeader(ctx, x, y, mouseX, mouseY);

        int contentY = y + HEADER_H;
        int contentH = PANEL_H - HEADER_H - FOOTER_H;

        switch (tab) {
            case GRACZ -> drawPlayers(ctx, x, contentY, PANEL_W, contentH, mouseX, mouseY);
            case VISUALS -> drawVisuals(ctx, x, contentY, PANEL_W, contentH, mouseX, mouseY);
            case USTAWIENIA -> drawSettings(ctx, x, contentY, PANEL_W, contentH, mouseX, mouseY);
        }

        drawFooter(ctx, x, y + PANEL_H - FOOTER_H, PANEL_W, FOOTER_H, mouseX, mouseY);
    }

    private void drawHeader(DrawContext ctx, int x, int y, int mouseX, int mouseY) {
        ctx.fill(x, y, x + PANEL_W, y + HEADER_H, 0xFF18181C);
        MinecraftClient client = MinecraftClient.getInstance();
        String name = client.getSession() != null ? client.getSession().getUsername() : "Gracz";
        ctx.drawText(this.textRenderer, Text.literal(name).formatted(Formatting.BOLD), x + 44, y + 10, 0xFFFFFFFF, false);
        ctx.drawText(this.textRenderer, Text.literal(tab.label).formatted(Formatting.GRAY), x + 44, y + 24, 0xFFAAAAAA, false);

        // avatar placeholder
        ctx.fill(x + 12, y + 8, x + 36, y + 32, GoatConfig.theme.accent);

        // zamkniecie
        boolean hoverClose = inside(mouseX, mouseY, x + PANEL_W - 28, y + 10, 18, 18);
        ctx.fill(x + PANEL_W - 28, y + 10, x + PANEL_W - 10, y + 28, hoverClose ? 0xFFAA3344 : 0x33FFFFFF);
        ctx.drawText(this.textRenderer, Text.literal("x"), x + PANEL_W - 23, y + 14, 0xFFFFFFFF, false);
    }

    private void drawPlayers(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        List<PlayerListEntry> list = players();
        int col = 0, row = 0;
        for (PlayerListEntry entry : list) {
            String pname = entry.getProfile().getName();
            int cardX = x + 16 + col * (CARD_W + GAP);
            int cardY = y + 10 + row * (CARD_H + GAP) - scroll;

            if (cardY + CARD_H >= y && cardY <= y + h) {
                boolean hover = inside(mouseX, mouseY, cardX, cardY, CARD_W, CARD_H);
                ctx.fill(cardX, cardY, cardX + CARD_W, cardY + CARD_H, hover ? 0xFF24242B : 0xFF1C1C21);

                // NOTE: oryginal rysowal tu glowke skina gracza przez
                // context.drawTexture(...) na PlayerSkinTextures z entry.getSkinTextures().
                // Tutaj uproszczone do kolorowego kwadratu - przywroc jesli chcesz 1:1 skiny.
                ctx.fill(cardX + 6, cardY + 6, cardX + 32, cardY + 32, 0xFF3A3A42);

                ctx.drawText(this.textRenderer, Text.literal(trim(pname, 14)), cardX + 40, cardY + 7, 0xFFFFFFFF, false);

                String sub = GoatConfig.showPing
                        ? (entry.getLatency() + " ms")
                        : "online";
                ctx.drawText(this.textRenderer, Text.literal(sub).formatted(Formatting.GRAY), cardX + 40, cardY + 20, 0xFF888888, false);

                // przycisk Search -> sprawdzenie nicku w bazie wyciekow
                int btnW = 64, btnH = 20;
                int btnX = cardX + CARD_W - btnW - 8;
                int btnY = cardY + (CARD_H - btnH) / 2;
                boolean btnHover = inside(mouseX, mouseY, btnX, btnY, btnW, btnH);
                ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnHover ? GoatConfig.theme.accentHover : GoatConfig.theme.accent);
                ctx.drawCenteredTextWithShadow(this.textRenderer, "Search", btnX + btnW / 2, btnY + 6, 0xFFFFFFFF);

                // wynik sprawdzenia (jesli juz sprawdzany)
                BreachChecker.Result res = BreachChecker.cached(pname);
                if (res != null) {
                    String label; int color;
                    switch (res.status) {
                        case LOADING -> { label = "Sprawdzanie..."; color = 0xFFAAAAAA; }
                        case FOUND -> { label = "W wycieku!"; color = 0xFFE05A5A; }
                        case CLEAN -> { label = "Czysty"; color = 0xFF5AE08F; }
                        default -> { label = "Blad"; color = 0xFFAAAAAA; }
                    }
                    ctx.drawText(this.textRenderer, Text.literal(label), cardX + 40, cardY + 32, color, false);
                }
            }

            col++;
            if (col >= 2) { col = 0; row++; }
        }

        ctx.drawText(this.textRenderer, Text.literal("Online: " + list.size()).formatted(Formatting.GRAY),
                x + 16, y + h - 2, 0xFF777777, false);
    }

    private void drawVisuals(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.literal("Motyw kolorystyczny"), x + 16, y + 10, 0xFFFFFFFF, false);

        int swatchSize = 48, gap = 14;
        int startX = x + 16;
        int startY = y + 36;
        GoatConfig.Theme[] themes = GoatConfig.Theme.values();
        for (int i = 0; i < themes.length; i++) {
            GoatConfig.Theme t = themes[i];
            int sx = startX + i * (swatchSize + gap);
            boolean hover = inside(mouseX, mouseY, sx, startY, swatchSize, swatchSize);
            boolean active = GoatConfig.theme == t;

            ctx.fill(sx - 3, startY - 3, sx + swatchSize + 3, startY + swatchSize + 3,
                    active ? 0xFFFFFFFF : (hover ? 0x55FFFFFF : 0x00000000));
            ctx.fill(sx, startY, sx + swatchSize, startY + swatchSize, t.accent);
            ctx.drawCenteredTextWithShadow(this.textRenderer, t.label, sx + swatchSize / 2, startY + swatchSize + 6, 0xFFCCCCCC);
        }

        ctx.drawText(this.textRenderer, Text.literal("Kliknij kolor, aby ustawic motyw calego GUI.")
                .formatted(Formatting.DARK_GRAY), x + 16, startY + swatchSize + 24, 0xFF777777, false);
    }

    private void drawSettings(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        ctx.drawText(this.textRenderer, Text.literal("Ustawienia"), x + 16, y + 10, 0xFFFFFFFF, false);

        String[] labels = { "Pokaz ping", "Pokaz koordynaty", "Kompaktowa lista", "Sortuj po nicku" };
        boolean[] values = { GoatConfig.showPing, GoatConfig.showCoords, GoatConfig.compactList, GoatConfig.sortByName };

        int rowY = y + 34;
        for (int i = 0; i < labels.length; i++) {
            int ry = rowY + i * 30;
            ctx.drawText(this.textRenderer, Text.literal(labels[i]), x + 16, ry + 5, 0xFFDDDDDD, false);

            int knobX = x + PANEL_W - 60;
            boolean on = values[i];
            boolean hover = inside(mouseX, mouseY, knobX, ry, 40, 18);
            ctx.fill(knobX, ry, knobX + 40, ry + 18, on ? GoatConfig.theme.accent : 0xFF333333);
            int dotX = on ? knobX + 22 : knobX + 2;
            ctx.fill(dotX, ry + 2, dotX + 16, ry + 16, hover ? 0xFFEEEEEE : 0xFFFFFFFF);
        }
    }

    private void drawFooter(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        ctx.fill(x, y, x + w, y + h, 0xFF18181C);
        Tab[] tabs = Tab.values();
        int tabW = w / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            int tx = x + i * tabW;
            boolean active = tab == tabs[i];
            boolean hover = inside(mouseX, mouseY, tx, y, tabW, h);
            int color = active ? GoatConfig.theme.accent : (hover ? 0xFF2A2A31 : 0x00000000);
            ctx.fill(tx, y, tx + tabW, y + h, color);
            ctx.drawCenteredTextWithShadow(this.textRenderer, tabs[i].label, tx + tabW / 2, y + h / 2 - 4,
                    active ? 0xFFFFFFFF : 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = px(), y = py();

        // zamkniecie
        if (inside(mouseX, mouseY, x + PANEL_W - 28, y + 10, 18, 18)) {
            this.close();
            return true;
        }

        // zakladki
        int footerY = y + PANEL_H - FOOTER_H;
        Tab[] tabs = Tab.values();
        int tabW = PANEL_W / tabs.length;
        if (inside(mouseX, mouseY, x, footerY, PANEL_W, FOOTER_H)) {
            int idx = (int) ((mouseX - x) / tabW);
            if (idx >= 0 && idx < tabs.length) {
                tab = tabs[idx];
                return true;
            }
        }

        if (tab == Tab.GRACZ) {
            return handlePlayerClick(mouseX, mouseY, x, y);
        } else if (tab == Tab.VISUALS) {
            return handleVisualsClick(mouseX, mouseY, x, y);
        } else if (tab == Tab.USTAWIENIA) {
            return handleSettingsClick(mouseX, mouseY, x, y);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handlePlayerClick(double mouseX, double mouseY, int x, int y) {
        List<PlayerListEntry> list = players();
        int contentY = y + HEADER_H;
        int col = 0, row = 0;
        for (PlayerListEntry entry : list) {
            int cardX = x + 16 + col * (CARD_W + GAP);
            int cardY = contentY + 10 + row * (CARD_H + GAP) - scroll;

            int btnW = 64, btnH = 20;
            int btnX = cardX + CARD_W - btnW - 8;
            int btnY = cardY + (CARD_H - btnH) / 2;
            if (inside(mouseX, mouseY, btnX, btnY, btnW, btnH)) {
                String nick = entry.getProfile().getName();
                searchedPlayer = nick;
                BreachChecker.checkAsync(nick);
                return true;
            }

            col++;
            if (col >= 2) { col = 0; row++; }
        }
        return false;
    }

    private boolean handleVisualsClick(double mouseX, double mouseY, int x, int y) {
        int contentY = y + HEADER_H;
        int swatchSize = 48, gap = 14;
        int startX = x + 16;
        int startY = contentY + 36;
        GoatConfig.Theme[] themes = GoatConfig.Theme.values();
        for (int i = 0; i < themes.length; i++) {
            int sx = startX + i * (swatchSize + gap);
            if (inside(mouseX, mouseY, sx, startY, swatchSize, swatchSize)) {
                GoatConfig.theme = themes[i];
                return true;
            }
        }
        return false;
    }

    private boolean handleSettingsClick(double mouseX, double mouseY, int x, int y) {
        int contentY = y + HEADER_H;
        int rowY = contentY + 34;
        int knobX = x + PANEL_W - 60;
        for (int i = 0; i < 4; i++) {
            int ry = rowY + i * 30;
            if (inside(mouseX, mouseY, knobX, ry, 40, 18)) {
                switch (i) {
                    case 0 -> GoatConfig.showPing = !GoatConfig.showPing;
                    case 1 -> GoatConfig.showCoords = !GoatConfig.showCoords;
                    case 2 -> GoatConfig.compactList = !GoatConfig.compactList;
                    case 3 -> GoatConfig.sortByName = !GoatConfig.sortByName;
                }
                return true;
            }
        }
        return false;
    }

    private String trim(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "." : s;
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
