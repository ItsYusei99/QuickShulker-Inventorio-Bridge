package com.itsyusei.sophisquicktorio.soph;

import com.itsyusei.sophisquicktorio.SophisQuickTorio;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.p3pp3rf1y.sophisticatedcore.api.IStorageWrapper;
import net.p3pp3rf1y.sophisticatedcore.common.gui.ISyncedContainer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler;
import net.p3pp3rf1y.sophisticatedcore.util.NoopStorageWrapper;

import java.util.Optional;

/**
 * Soph Storage menu bound to an item stack instead of a block entity.
 * Slot layout, scrolling, upgrades and server&lt;-&gt;client sync all come
 * from Soph's own StorageContainerMenuBase; only the backing wrapper
 * (StackStorageWrapper over the host stack) and the validity check differ.
 */
public class ItemBackedShulkerMenu extends StorageContainerMenuBase<IStorageWrapper>
        implements ISyncedContainer {
    private final SophShulker.Host host;
    private final Item initialItem;
    private final Component title;

    public ItemBackedShulkerMenu(MenuType<?> type, int windowId, Player player,
            IStorageWrapper wrapper, SophShulker.Host host, ItemStack hostStack) {
        super(type, windowId, player, wrapper, NoopStorageWrapper.INSTANCE, -1, false);
        this.host = host;
        this.initialItem = hostStack.isEmpty() ? null : hostStack.getItem();
        Component hoverName = Component.literal("Shulker Box");
        try {
            if (!hostStack.isEmpty()) {
                hoverName = hostStack.getHoverName();
            }
        } catch (Throwable ignored) {
        }
        this.title = hoverName;
    }

    @Override
    public Optional<BlockPos> getBlockPosition() {
        return Optional.empty();
    }

    @Override
    public Optional<Entity> getEntity() {
        return Optional.empty();
    }

    @Override
    protected StorageUpgradeSlot instantiateUpgradeSlot(UpgradeHandler upgradeHandler, int slotIndex) {
        return new StorageUpgradeSlot(upgradeHandler, slotIndex);
    }

    @Override
    public void openSettings() {
        // Item-backed shulkers expose no settings menu.
    }

    @Override
    public boolean detectSettingsChangeAndReload() {
        return false;
    }

    @Override
    protected boolean storageItemHasChanged() {
        boolean changed;
        try {
            if (host.kind == SophShulker.Host.MENU_SLOT) {
                // Nested open: the server holds the stack reference directly.
                changed = initialItem == null;
            } else if (initialItem == null) {
                changed = true;
            } else {
                ItemStack current = host.resolve(player);
                changed = current.isEmpty() || current.getItem() != initialItem;
            }
        } catch (Throwable t) {
            changed = true;
        }
        if (changed) {
            SophisQuickTorio.LOGGER.info("[sqt][dbg] storageItemHasChanged kind={} (side={})",
                    host.kind, player.level().isClientSide() ? "client" : "server");
        }
        return changed;
    }

    @Override
    public boolean stillValid(Player player) {
        boolean ok;
        try {
            ok = player.isAlive() && !storageItemHasChanged();
        } catch (Throwable t) {
            SophisQuickTorio.LOGGER.info("[sqt][dbg] stillValid threw: {}", t.toString());
            ok = false;
        }
        if (!ok) {
            SophisQuickTorio.LOGGER.info("[sqt][dbg] stillValid=false kind={} (side={})",
                    host.kind, player.level().isClientSide() ? "client" : "server");
        }
        return ok;
    }

    @Override
    public void removed(Player player) {
        try {
            SophisQuickTorio.LOGGER.info("[sqt][dbg] menu removed kind={}", host.kind);
        } catch (Throwable ignored) {
        }
        super.removed(player);
    }

    @Override
    public void handlePacket(net.minecraft.nbt.CompoundTag tag) {
        try {
            super.handlePacket(tag);
        } catch (Throwable t) {
            SophisQuickTorio.LOGGER.debug("[sqt] soph menu packet ignored: {}", t.toString());
        }
    }

    /** Keep Soph's quick-move behavior; just guard the host stack move. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        try {
            Slot slot = index >= 0 && index < slots.size() ? slots.get(index) : null;
            ItemStack hostNow = host.resolve(player);
            if (slot != null && !hostNow.isEmpty() && slot.getItem() == hostNow) {
                // Never shift-click the open shulker into itself.
                return ItemStack.EMPTY;
            }
        } catch (Throwable t) {
            // fall through to default behavior
        }
        return super.quickMoveStack(player, index);
    }

    public Component getTitle() {
        return title;
    }
}
