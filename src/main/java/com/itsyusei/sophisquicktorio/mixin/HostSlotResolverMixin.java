package com.itsyusei.sophisquicktorio.mixin;

import com.ice2974.quickshulkerneoforged.common.open.HostSlotRef;
import com.ice2974.quickshulkerneoforged.common.open.HostStorageScope;
import com.ice2974.quickshulkerneoforged.neoforge.NeoForgeHostSlotResolver;
import com.itsyusei.sophisquicktorio.SophisQuickTorio;
import de.rubixdev.inventorio.api.InventorioAPI;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Extends QuickShulker slot resolution to Inventorio addon slots.
 * Every hook is fail-open (require = 0 + try/catch): if QuickShulker or
 * Inventorio change or are absent, the game behaves exactly as without
 * this bridge.
 */
@Mixin(value = NeoForgeHostSlotResolver.class, remap = false)
public class HostSlotResolverMixin {

    private static boolean isInventorioSlot(Slot slot) {
        try {
            String name = slot.getClass().getName();
            return name.startsWith("de.rubixdev.inventorio.");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isInventorioMenu(AbstractContainerMenu menu) {
        try {
            String name = menu.getClass().getName();
            return name.startsWith("de.rubixdev.inventorio.");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isInventorioContainer(Object container) {
        try {
            String name = container.getClass().getName();
            return name.startsWith("de.rubixdev.inventorio.");
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean isSophMenu(AbstractContainerMenu menu) {
        try {
            String name = menu.getClass().getName();
            return name.startsWith("net.p3pp3rf1y.sophisticated");
        } catch (Throwable t) {
            return false;
        }
    }

    /** Marker protocol: menu-independent refs use menuSlotIndex == -1. */
    private static boolean isBridgeRef(HostSlotRef ref) {
        try {
            return ref != null && ref.scope() == HostStorageScope.PLAYER_CONTAINER_MENU
                    && ref.menuSlotIndex() == -1;
        } catch (Throwable t) {
            return false;
        }
    }

    private static PlayerInventoryAddon addonOf(Player player) {
        try {
            if (player == null || player.level() == null || player.level().isClientSide()) {
                return null;
            }
            return InventorioAPI.getInventoryAddon(player);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Client (and bundling entry): resolve shulkers in Inventorio rows. */
    @Inject(method = "forPlayerInventorySlot", at = @At("RETURN"), cancellable = true, require = 0)
    private static void sqt$onResolve(Player player, AbstractContainerMenu menu, Slot slot,
            CallbackInfoReturnable<Optional<HostSlotRef>> cir) {
        try {
            if (cir.getReturnValue() != null && cir.getReturnValue().isPresent()) {
                return;
            }
            if (player == null || menu == null || slot == null) {
                return;
            }
            if (!isInventorioSlot(slot)) {
                return;
            }
            ItemStack stack;
            try {
                stack = slot.getItem();
            } catch (Throwable t) {
                return;
            }
            if (stack == null || stack.isEmpty()) {
                return;
            }
            int containerIdx;
            try {
                containerIdx = slot.getContainerSlot();
            } catch (Throwable t) {
                return;
            }
            SophisQuickTorio.LOGGER.info("[sqt] resolve Inventorio slot {} ({})",
                    containerIdx, stack.getItem());
            cir.setReturnValue(Optional.of(new HostSlotRef(
                    HostStorageScope.PLAYER_CONTAINER_MENU, containerIdx, -1)));
        } catch (Throwable t) {
            // fail-open: keep vanilla behavior
        }
    }

    /** Client: menu-slot refs for slots inside Soph screens (storage area). */
    @Inject(method = "forPlayerInventorySlot", at = @At("RETURN"), cancellable = true, require = 0)
    private static void sqt$onResolveSophMenu(Player player, AbstractContainerMenu menu, Slot slot,
            CallbackInfoReturnable<Optional<HostSlotRef>> cir) {
        try {
            if (cir.getReturnValue() != null && cir.getReturnValue().isPresent()) {
                return;
            }
            if (player == null || menu == null || slot == null) {
                return;
            }
            if (!isSophMenu(menu)) {
                return;
            }
            ItemStack stack;
            try {
                stack = slot.getItem();
            } catch (Throwable t) {
                return;
            }
            if (stack == null || stack.isEmpty()) {
                return;
            }
            int menuIdx;
            try {
                menuIdx = slot.index;
            } catch (Throwable t) {
                return;
            }
            int logical = menuIdx;
            try {
                logical = slot.getContainerSlot();
            } catch (Throwable t) {
                // keep menu index
            }
            SophisQuickTorio.LOGGER.info("[sqt] resolve Soph menu slot {} ({})",
                    menuIdx, stack.getItem());
            cir.setReturnValue(Optional.of(new HostSlotRef(
                    HostStorageScope.PLAYER_CONTAINER_MENU, logical, menuIdx)));
        } catch (Throwable t) {
            // fail-open: keep vanilla behavior
        }
    }

    /** Server: resolve bridge refs straight from the addon (menu-independent). */
    @Inject(method = "resolve", at = @At("RETURN"), cancellable = true, require = 0)
    private static void sqt$onResolveStack(Player player, HostSlotRef ref,
            CallbackInfoReturnable<ItemStack> cir) {
        try {
            ItemStack cur = cir.getReturnValue();
            if (cur != null && !cur.isEmpty()) {
                return;
            }
            if (!isBridgeRef(ref)) {
                return;
            }
            PlayerInventoryAddon addon = addonOf(player);
            if (addon == null) {
                return;
            }
            int idx = ref.logicalSlotIndex();
            Container c = addon;
            if (idx < 0 || idx >= c.getContainerSize()) {
                return;
            }
            ItemStack found = c.getItem(idx);
            if (found == null || found.isEmpty()) {
                return;
            }
            SophisQuickTorio.LOGGER.info("[sqt] resolve addon idx={} -> {}",
                    idx, found.getItem());
            cir.setReturnValue(found);
        } catch (Throwable t) {
            // fail-open
        }
    }

    /** Server: write back through the addon for bridge refs. */
    @Inject(method = "set", at = @At("HEAD"), cancellable = true, require = 0)
    private static void sqt$onSet(Player player, HostSlotRef ref, ItemStack stack,
            CallbackInfo ci) {
        try {
            if (!isBridgeRef(ref)) {
                return;
            }
            PlayerInventoryAddon addon = addonOf(player);
            if (addon == null) {
                return;
            }
            int idx = ref.logicalSlotIndex();
            Container c = addon;
            if (idx < 0 || idx >= c.getContainerSize()) {
                return;
            }
            SophisQuickTorio.LOGGER.info("[sqt] set addon idx={} -> {}",
                    idx, stack == null ? "null" : stack.getItem());
            c.setItem(idx, stack == null ? ItemStack.EMPTY : stack);
            ci.cancel();
        } catch (Throwable t) {
            // fail-open
        }
    }

    /** Server: accept Inventorio addon containers when resolving menu slots. */    @Inject(method = "resolveMenuSlot", at = @At("HEAD"), cancellable = false, require = 0)
    private static void sqt$onResolveMenuHead(AbstractContainerMenu menu, HostSlotRef ref,
            CallbackInfoReturnable<Optional<Slot>> cir) {
        try {
            SophisQuickTorio.LOGGER.info("[sqt] resolveMenuSlot menu={} idx={} slots={}",
                    menu == null ? "null" : menu.getClass().getName(),
                    ref == null ? -999 : ref.menuSlotIndex(),
                    menu == null ? -1 : menu.slots.size());
            if (menu != null && ref != null && ref.menuSlotIndex() >= 0
                    && ref.menuSlotIndex() < menu.slots.size()) {
                net.minecraft.world.inventory.Slot s = menu.slots.get(ref.menuSlotIndex());
                SophisQuickTorio.LOGGER.info("[sqt] slot={} container={} item={}",
                        s == null ? "null" : s.getClass().getName(),
                        (s == null || s.container == null) ? "null" : s.container.getClass().getName(),
                        (s == null) ? "?" : String.valueOf(s.getItem().getItem()));
            }
        } catch (Throwable t) {
            // fail-open
        }
    }

    @Inject(method = "resolveMenuSlot", at = @At("RETURN"), cancellable = true, require = 0)
    private static void sqt$onResolveMenu(AbstractContainerMenu menu, HostSlotRef ref,
            CallbackInfoReturnable<Optional<Slot>> cir) {
        try {
            if (cir.getReturnValue() != null && cir.getReturnValue().isPresent()) {
                return;
            }
            if (menu == null || ref == null) {
                return;
            }
            // Nuestras refs usan menuSlotIndex == -1: buscar por índice de contenedor.
            if (!isBridgeRef(ref)) {
                return;
            }
            int want = ref.logicalSlotIndex();
            for (Slot s : menu.slots) {
                try {
                    if (s != null && s.container != null && isInventorioSlot(s)
                            && s.getContainerSlot() == want) {
                        SophisQuickTorio.LOGGER.info("[sqt] bundling slot inventorio idx={}", want);
                        cir.setReturnValue(Optional.of(s));
                        return;
                    }
                } catch (Throwable t) {
                    // seguir buscando
                }
            }
        } catch (Throwable t) {
            // fail-open
        }
    }

    /** Server: treat Inventorio and Soph menus as supported bundling menus. */
    @Inject(method = "isSupportedBundlingMenu", at = @At("RETURN"), cancellable = true, require = 0)
    private static void sqt$onSupported(AbstractContainerMenu menu,
            CallbackInfoReturnable<Boolean> cir) {
        try {
            if (cir.getReturnValue() != null && cir.getReturnValue()) {
                return;
            }
            if (isInventorioMenu(menu) || isSophMenu(menu)) {
                cir.setReturnValue(true);
            }
        } catch (Throwable t) {
            // fail-open
        }
    }
}
