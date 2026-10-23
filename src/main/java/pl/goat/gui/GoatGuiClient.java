package pl.goat.gui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

public class GoatGuiClient implements ClientModInitializer {

    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.goatgui.open",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_EQUAL, // "=" -- ten sam domyslny klawisz co w oryginale
                "category.goatgui"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new GoatScreen());
                }
            }
        });
    }
}
