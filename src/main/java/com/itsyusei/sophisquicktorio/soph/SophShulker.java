package com.itsyusei.sophisquicktorio.soph;

import com.itsyusei.sophisquicktorio.SophisQuickTorio;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers for Sophisticated Storage shulker support.
 * Detection is string-based so nothing breaks if Soph is absent.
 */
public final class SophShulker {
    public static final String SOPH_MOD_ID = "sophisticatedstorage";
    /** Class name of Soph's shulker item (all 6 tiers share it). */
    private static final String SHULKER_ITEM_CLASS =
            "net.p3pp3rf1y.sophisticatedstorage.item.ShulkerBoxItem";

    private SophShulker() {
    }

    public static boolean isSophShulker(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            return SHULKER_ITEM_CLASS.equals(stack.getItem().getClass().getName());
        } catch (Throwable t) {
            return false;
        }
    }

    /** Where the host shulker lives. Encoded on the wire as (kind, slot, data). */
    public static final class Host {
        public static final byte MAIN_HAND = 0;
        public static final byte OFF_HAND = 1;
        public static final byte INV_SLOT = 2;
        public static final byte ADDON_SLOT = 3;
        /** Slot of another open menu (e.g. nested inside a Soph screen). data = containerId. */
        public static final byte MENU_SLOT = 4;

        public final byte kind;
        public final int slot;
        public final int data;

        public Host(byte kind, int slot) {
            this(kind, slot, -1);
        }

        public Host(byte kind, int slot, int data) {
            this.kind = kind;
            this.slot = slot;
            this.data = data;
        }

        public static Optional<Host> fromHand(InteractionHand hand) {
            return Optional.of(new Host(hand == InteractionHand.OFF_HAND ? OFF_HAND : MAIN_HAND, 0));
        }

        private static boolean isInventorioSlot(Slot slot) {
            try {
                return slot.getClass().getName().startsWith("de.rubixdev.inventorio.");
            } catch (Throwable t) {
                return false;
            }
        }

        /**
         * Player-inventory slots (container slots 0-35, 40 = offhand) plus
         * Inventorio addon slots (resolved server-side through the addon).
         */
        public static Optional<Host> fromSlot(Player player, Slot slot) {
            try {
                if (player == null || slot == null) {
                    return Optional.empty();
                }
                if (slot.container != null && slot.container == player.getInventory()) {
                    int cs = slot.getContainerSlot();
                    if (cs == Inventory.SLOT_OFFHAND) {
                        return Optional.of(new Host(OFF_HAND, 0));
                    }
                    if (cs >= 0 && cs < Inventory.getSelectionSize() + 27) {
                        return Optional.of(new Host(INV_SLOT, cs));
                    }
                    return Optional.empty();
                }
                if (isInventorioSlot(slot)) {
                    int cs;
                    try {
                        cs = slot.getContainerSlot();
                    } catch (Throwable t) {
                        return Optional.empty();
                    }
                    if (cs >= 0) {
                        return Optional.of(new Host(ADDON_SLOT, cs));
                    }
                }
                return Optional.empty();
            } catch (Throwable t) {
                return Optional.empty();
            }
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeByte(kind);
            buf.writeInt(slot);
            buf.writeInt(data);
        }

        public static Host read(FriendlyByteBuf buf) {
            return new Host(buf.readByte(), buf.readInt(), buf.readInt());
        }

        public ItemStack resolve(Player player) {
            try {
                if (player == null) {
                    return ItemStack.EMPTY;
                }
                if (kind == MAIN_HAND) {
                    return player.getMainHandItem();
                }
                if (kind == OFF_HAND) {
                    return player.getOffhandItem();
                }
                if (kind == ADDON_SLOT) {
                    return resolveAddon(player, slot);
                }
                if (kind == MENU_SLOT) {
                    return resolveMenuSlot(player);
                }
                Inventory inv = player.getInventory();
                if (slot < 0 || slot >= inv.getContainerSize()) {
                    return ItemStack.EMPTY;
                }
                return inv.getItem(slot);
            } catch (Throwable t) {
                return ItemStack.EMPTY;
            }
        }

        /** Inventorio addon lookup, works on both sides (string-based, no hard dep). */
        private static ItemStack resolveAddon(Player player, int idx) {
            try {
                Object addon = Class
                        .forName("de.rubixdev.inventorio.api.InventorioAPI")
                        .getMethod("getInventoryAddon", Player.class)
                        .invoke(null, player);
                if (!(addon instanceof net.minecraft.world.Container container)) {
                    return ItemStack.EMPTY;
                }
                if (idx < 0 || idx >= container.getContainerSize()) {
                    return ItemStack.EMPTY;
                }
                ItemStack found = container.getItem(idx);
                return found == null ? ItemStack.EMPTY : found;
            } catch (Throwable t) {
                return ItemStack.EMPTY;
            }
        }

        /** Slot of the currently open menu (nested open); data carries the containerId. */
        private ItemStack resolveMenuSlot(Player player) {
            try {
                if (player.containerMenu == null || player.containerMenu.containerId != data) {
                    return ItemStack.EMPTY;
                }
                if (slot < 0 || slot >= player.containerMenu.slots.size()) {
                    return ItemStack.EMPTY;
                }
                ItemStack found = player.containerMenu.getSlot(slot).getItem();
                return found == null ? ItemStack.EMPTY : found;
            } catch (Throwable t) {
                return ItemStack.EMPTY;
            }
        }
    }

    /** QuickShulker activation key, read from its own config (fail-open to K). */
    public static final class QuickShulkerKeys {
        private static final int DEFAULT_KEY = 75; // GLFW_KEY_K
        private static int activationKey = -1;
        private static Boolean rightClickInInventory;
        private static Boolean rightClickToOpen;

        public static int activationKey() {
            if (activationKey < 0) {
                activationKey = readKey();
            }
            return activationKey;
        }

        public static boolean rightClickInInventory() {
            if (rightClickInInventory == null) {
                rightClickInInventory = readBool("rightClickInInventory", true);
            }
            return rightClickInInventory;
        }

        public static boolean rightClickToOpen() {
            if (rightClickToOpen == null) {
                rightClickToOpen = readBool("rightClickToOpen", true);
            }
            return rightClickToOpen;
        }

        private static String configText() {
            try {
                Path cfg = Minecraft.getInstance().gameDirectory.toPath()
                        .resolve("config").resolve("quickshulker_neoforged-common.toml");
                if (!Files.isRegularFile(cfg)) {
                    return "";
                }
                return Files.readString(cfg);
            } catch (Throwable t) {
                return "";
            }
        }

        private static int readKey() {
            try {
                Matcher m = Pattern.compile("activationKey\\s*=\\s*\"([^\"]+)\"")
                        .matcher(configText());
                if (m.find()) {
                    int v = com.mojang.blaze3d.platform.InputConstants
                            .getKey(m.group(1)).getValue();
                    if (v > 0) {
                        return v;
                    }
                }
            } catch (Throwable t) {
                SophisQuickTorio.LOGGER.debug("[sqt] QS key parse failed, using K");
            }
            return DEFAULT_KEY;
        }

        private static boolean readBool(String key, boolean def) {
            try {
                Matcher m = Pattern.compile(key + "\\s*=\\s*(true|false)")
                        .matcher(configText());
                if (m.find()) {
                    return Boolean.parseBoolean(m.group(1));
                }
            } catch (Throwable t) {
                // default
            }
            return def;
        }
    }
}
