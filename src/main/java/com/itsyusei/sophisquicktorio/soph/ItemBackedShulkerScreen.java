package com.itsyusei.sophisquicktorio.soph;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;

/** Client screen for the item-backed Soph shulker menu (full Soph rendering). */
public class ItemBackedShulkerScreen extends StorageScreenBase<ItemBackedShulkerMenu> {
    public ItemBackedShulkerScreen(ItemBackedShulkerMenu menu, Inventory playerInventory,
            Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected String getStorageSettingsTabTooltip() {
        return "";
    }
}
