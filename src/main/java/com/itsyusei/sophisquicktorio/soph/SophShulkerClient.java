package com.itsyusei.sophisquicktorio.soph;

import com.itsyusei.sophisquicktorio.SophisQuickTorio;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Optional;

/**
 * Client-side triggers for Soph shulker quick-open. Mirrors QuickShulker
 * behavior (same key, right-click) but only fires for Soph shulker items,
 * which QuickShulker itself ignores — so the two never overlap.
 */
public final class SophShulkerClient {
    private SophShulkerClient() {
    }

    public static void init(IEventBus modBus) {
        try {
            modBus.addListener(SophShulkerClient::registerScreens);
            NeoForge.EVENT_BUS.register(SophShulkerClient.class);
            SophisQuickTorio.LOGGER.info("[sqt] Soph shulker client hooks active");
        } catch (Throwable t) {
            SophisQuickTorio.LOGGER.warn("[sqt] Soph client init failed: {}", t.toString());
        }
    }

    public static void registerScreens(RegisterMenuScreensEvent event) {
        try {
            event.register(SophMenus.ITEM_SHULKER.get(), ItemBackedShulkerScreen::new);
        } catch (Throwable t) {
            SophisQuickTorio.LOGGER.warn("[sqt] Soph screen registration failed: {}", t.toString());
        }
    }

    private static void send(SophShulker.Host host) {
        try {
            PacketDistributor.sendToServer(new OpenSophShulkerPayload(host));
        } catch (Throwable t) {
            SophisQuickTorio.LOGGER.warn("[sqt] Soph open send failed: {}", t.toString());
        }
    }

    private static Optional<SophShulker.Host> hoveredHost(Player player, Screen screen) {
        try {
            if (!(screen instanceof AbstractContainerScreen<?> gui)) {
                return Optional.empty();
            }
            Slot slot;
            try {
                slot = gui.getSlotUnderMouse();
            } catch (Throwable t) {
                return Optional.empty();
            }
            if (slot == null || !slot.hasItem()) {
                return Optional.empty();
            }
            ItemStack stack = slot.getItem();
            debugSlot(screen, slot, stack);
            if (!SophShulker.isSophShulker(stack)) {
                return Optional.empty();
            }
            Optional<SophShulker.Host> host = SophShulker.Host.fromSlot(player, slot);
            if (host.isPresent()) {
                return host;
            }
            // Nested inside another open menu (e.g. Soph storage area):
            // reference the menu slot; the server captures the live stack.
            try {
                if (player.containerMenu != null
                        && slot.index >= 0 && slot.index < player.containerMenu.slots.size()
                        && player.containerMenu.getSlot(slot.index) == slot) {
                    return Optional.of(new SophShulker.Host(SophShulker.Host.MENU_SLOT,
                            slot.index, player.containerMenu.containerId));
                }
            } catch (Throwable ignored) {
            }
            return Optional.empty();
        } catch (Throwable t) {
            return Optional.empty();
        }
    }

    /** Temporary diagnostics for Soph/Inventorio screens. */
    private static void debugSlot(Screen screen, Slot slot, ItemStack stack) {
        try {
            String screenName = screen.getClass().getName();
            boolean interesting = screenName.startsWith("net.p3pp3rf1y")
                    || screenName.startsWith("de.rubixdev.inventorio")
                    || SophShulker.isSophShulker(stack);
            if (!interesting) {
                return;
            }
            Object container = slot.container;
            int cs = -999;
            try {
                cs = slot.getContainerSlot();
            } catch (Throwable ignored) {
            }
            SophisQuickTorio.LOGGER.info(
                    "[sqt][dbg] screen={} slot={} container={} cslot={} item={}",
                    screenName, slot.getClass().getName(),
                    container == null ? "null" : container.getClass().getName(), cs,
                    stack.getItem());
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || event.getKeyCode() != SophShulker.QuickShulkerKeys.activationKey()) {
                return;
            }
            Optional<SophShulker.Host> host = hoveredHost(mc.player, event.getScreen());
            if (host.isPresent()) {
                send(host.get());
                event.setCanceled(true);
            }
        } catch (Throwable t) {
            // fail-open
        }
    }

    @SubscribeEvent
    public static void onScreenRightClick(ScreenEvent.MouseButtonPressed.Pre event) {
        try {
            // GLFW_MOUSE_BUTTON_RIGHT == 1
            if (event.getButton() != 1) {
                return;
            }
            if (!SophShulker.QuickShulkerKeys.rightClickInInventory()) {
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            Optional<SophShulker.Host> host = hoveredHost(mc.player, event.getScreen());
            if (host.isPresent()) {
                send(host.get());
                event.setCanceled(true);
            }
        } catch (Throwable t) {
            // fail-open
        }
    }

    @SubscribeEvent
    public static void onHandRightClick(PlayerInteractEvent.RightClickItem event) {
        try {
            if (!event.getLevel().isClientSide()) {
                return;
            }
            if (!SophShulker.QuickShulkerKeys.rightClickToOpen()) {
                return;
            }
            Player player = event.getEntity();
            // Shift + right-click keeps the vanilla behavior (place the block).
            if (player.isShiftKeyDown()) {
                return;
            }
            ItemStack stack = event.getItemStack();
            if (!SophShulker.isSophShulker(stack)) {
                return;
            }
            InteractionHand hand = event.getHand();
            Optional<SophShulker.Host> host = SophShulker.Host.fromHand(hand);
            if (host.isPresent()) {
                send(host.get());
                event.setCanceled(true);
                event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        } catch (Throwable t) {
            // fail-open
        }
    }
}
