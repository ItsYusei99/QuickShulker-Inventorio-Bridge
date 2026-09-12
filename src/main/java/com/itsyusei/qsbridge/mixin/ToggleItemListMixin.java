package com.itsyusei.qsbridge.mixin;

import com.dplayend.togenc.handler.HandlerToggleEnchantments;
import com.dplayend.togenc.util.ToggleItem;
import com.itsyusei.qsbridge.QSBridge;
import de.rubixdev.inventorio.api.InventorioAPI;
import de.rubixdev.inventorio.player.PlayerInventoryAddon;
import de.rubixdev.inventorio.player.inventory.PlayerInventoryExtension;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Extends Toggle Enchantments' item list with Inventorio's ToolBelt slots,
 * so tools stored there can have their enchantments toggled too.
 *
 * Only the ToolBelt section is included (the tool rows the user asked
 * for): it is small, fixed-size and ordered identically on client and
 * server, which keeps the index-based toggle protocol consistent.
 *
 * Fail-open (require = 0 + try/catch): if anything is missing, the
 * vanilla 6-entry list is preserved untouched.
 */
@Mixin(value = HandlerToggleEnchantments.class, remap = false)
public class ToggleItemListMixin {

    @Inject(method = "itemList", at = @At("RETURN"), cancellable = true, require = 0)
    private static void qsbridge$onItemList(Player player,
            CallbackInfoReturnable<List<ToggleItem>> cir) {
        try {
            List<ToggleItem> list = cir.getReturnValue();
            if (list == null || player == null) {
                return;
            }
            PlayerInventoryAddon addon;
            try {
                addon = InventorioAPI.getInventoryAddon(player);
            } catch (Throwable t) {
                return;
            }
            if (addon == null) {
                return;
            }
            List<ItemStack> belt;
            try {
                belt = ((PlayerInventoryExtension) addon).toolBelt;
            } catch (Throwable t) {
                return;
            }
            if (belt == null || belt.isEmpty()) {
                return;
            }
            ResourceLocation emptyTex;
            try {
                emptyTex = ResourceLocation.fromNamespaceAndPath(
                        "togenc", "textures/item/empty_armor_slot_sword.png");
            } catch (Throwable t) {
                return;
            }
            int added = 0;
            for (ItemStack stack : belt) {
                ItemStack s;
                try {
                    s = stack;
                } catch (Throwable t) {
                    continue;
                }
                if (s == null || s.isEmpty()) {
                    continue;
                }
                // Evitar duplicados con lo que togenc ya lista (armadura/manos).
                boolean dup = false;
                try {
                    for (ToggleItem existing : list) {
                        if (existing != null && existing.stack() != null
                                && ItemStack.isSameItemSameComponents(existing.stack(), s)) {
                            dup = true;
                            break;
                        }
                    }
                } catch (Throwable t) {
                    dup = false;
                }
                if (dup) {
                    continue;
                }
                try {
                    list.add(new ToggleItem(s, 9, 157 + added * 18, emptyTex));
                    added++;
                } catch (Throwable t) {
                    // seguir con el siguiente slot
                }
            }
            if (added > 0) {
                QSBridge.LOGGER.info("[qsbridge] togenc: {} herramientas de Inventorio añadidas",
                        added);
            }
            cir.setReturnValue(list);
        } catch (Throwable t) {
            // fail-open: lista vainilla intacta
        }
    }
}
