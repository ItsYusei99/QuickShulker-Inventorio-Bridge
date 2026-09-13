package com.itsyusei.sophisquicktorio.soph;

import com.itsyusei.sophisquicktorio.SophisQuickTorio;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedstorage.item.StackStorageWrapper;

/** MenuType registration plus server-side open logic for item-backed Soph shulkers. */
public final class SophMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, SophisQuickTorio.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<ItemBackedShulkerMenu>> ITEM_SHULKER =
            MENUS.register("soph_shulker", () -> IMenuTypeExtension
                    .<ItemBackedShulkerMenu>create(SophMenus::createMenu));

    private SophMenus() {
    }

    private static ItemBackedShulkerMenu createMenu(int windowId, Inventory inv,
            net.minecraft.network.RegistryFriendlyByteBuf buf) {
        return fromBuffer(windowId, inv, buf);
    }

    private static ItemBackedShulkerMenu fromBuffer(int windowId, Inventory inv,
            net.minecraft.network.RegistryFriendlyByteBuf buf) {
        Player player = inv.player;
        SophShulker.Host host;
        ItemStack snapshot = ItemStack.EMPTY;
        try {
            host = SophShulker.Host.read(buf);
            snapshot = net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
        } catch (Throwable t) {
            host = new SophShulker.Host(SophShulker.Host.MAIN_HAND, 0);
        }
        IStorageWrapper wrapper = null;
        try {
            if (!snapshot.isEmpty() && SophShulker.isSophShulker(snapshot)) {
                wrapper = StackStorageWrapper.fromStack(player.level().registryAccess(), snapshot);
            }
        } catch (Throwable t) {
            SophisQuickTorio.LOGGER.warn("[sqt][dbg] client wrapper build failed: {}", t.toString());
        }
        SophisQuickTorio.LOGGER.info("[sqt][dbg] fromBuffer kind={} slot={} data={} snapshot={} wrapper={}",
                host.kind, host.slot, host.data, snapshot.getItem(),
                wrapper == null ? "null" : wrapper.getClass().getName());
        if (wrapper == null) {
            wrapper = net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper.INSTANCE;
        }
        return new ItemBackedShulkerMenu(ITEM_SHULKER.get(), windowId, player, wrapper, host, snapshot);
    }

    /** Server-side: validate the host stack and open the item menu. */
    public static void open(ServerPlayer player, SophShulker.Host host) {
        try {
            ItemStack stack = host.resolve(player);
            if (stack.isEmpty() || !SophShulker.isSophShulker(stack)) {
                return;
            }
            IStorageWrapper wrapper = StackStorageWrapper.fromStack(
                    player.serverLevel().registryAccess(), stack);
            net.minecraft.network.chat.Component title;
            try {
                title = wrapper.getDisplayName();
            } catch (Throwable t) {
                title = stack.getHoverName();
            }
            final net.minecraft.network.chat.Component finalTitle = title;
            final ItemStack snapshot = stack.copy();
            player.openMenu(new SimpleMenuProvider(
                            (windowId, inv, p) -> new ItemBackedShulkerMenu(
                                    ITEM_SHULKER.get(), windowId, p, wrapper, host, stack),
                            finalTitle),
                    buf -> {
                        host.write(buf);
                        net.minecraft.world.item.ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, snapshot);
                    });
            SophisQuickTorio.LOGGER.info("[sqt] opened Soph shulker for {}", player.getGameProfile().getName());
        } catch (Throwable t) {
            SophisQuickTorio.LOGGER.warn("[sqt] failed to open Soph shulker: {}", t.toString());
        }
    }
}
