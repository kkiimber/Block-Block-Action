package ru.zhuma.blockblockaction;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Client control for choosing the mode applied by the protector wand. */
@EventBusSubscriber(modid = BlockBlockAction.MOD_ID, value = Dist.CLIENT)
public final class ClientWandControls {
    private static final String CATEGORY = "key.categories.blockblockaction";
    private static final KeyMapping MODE_MODIFIER = new KeyMapping(
            "key.blockblockaction.mode_modifier",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY
    );
    private static ProtectionState selectedMode = ProtectionState.BREAK_ONLY;

    @SubscribeEvent
    public static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (MODE_MODIFIER.isDown() && isHoldingWand()) {
            changeMode(event.getScrollDeltaY() > 0.0D ? 1 : -1);
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS || !MODE_MODIFIER.isDown() || !isHoldingWand()) {
            return;
        }
        if (event.getKey() == GLFW.GLFW_KEY_COMMA) {
            changeMode(-1);
        } else if (event.getKey() == GLFW.GLFW_KEY_PERIOD) {
            changeMode(1);
        }
    }

    public static ProtectionState selectedMode() {
        return selectedMode;
    }

    private static void changeMode(int direction) {
        ProtectionState[] modes = ProtectionState.values();
        int next = Math.floorMod(selectedMode.ordinal() + direction, modes.length);
        selectedMode = modes[next];
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.translatable(selectedMode.displayKey()), true);
        }
        PacketDistributor.sendToServer(new ModeSelectionPayload(selectedMode.ordinal()));
    }

    private static boolean isHoldingWand() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && minecraft.player.getMainHandItem().is(ModItems.PROTECTOR_WAND.get());
    }

    private ClientWandControls() {
    }

    @EventBusSubscriber(modid = BlockBlockAction.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBusEvents {
        @SubscribeEvent
        public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(MODE_MODIFIER);
        }

        private ModBusEvents() {
        }
    }
}
