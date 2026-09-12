package com.itsyusei.qsbridge.mixin;

import com.dplayend.togenc.client.ToggleScreen;
import com.dplayend.togenc.handler.HandlerToggleEnchantments;
import com.dplayend.togenc.util.ButtonWidget;
import com.dplayend.togenc.util.ToggleItem;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Client-only: sizes Toggle Enchantments' screen for the extra Inventorio
 * tool entries appended by {@code ToggleItemListMixin}.
 *
 * The vanilla screen allocates exactly 6 item buttons; any more entries
 * crash it with ArrayIndexOutOfBoundsException. Extra tools render in a
 * second column (x=29) inside the visible panel area, so no background
 * drawing is touched.
 *
 * Fail-open (require = 0 + try/catch).
 */
@Mixin(value = ToggleScreen.class, remap = false)
public class ToggleScreenMixin {

    @Shadow
    public ButtonWidget[] chooseItem;

    @Shadow
    public int imageHeight;

    @Shadow
    public LocalPlayer player;

    @Inject(method = "init", at = @At("HEAD"), require = 0)
    private void qsbridge$onInitHead(CallbackInfo ci) {
        try {
            List<ToggleItem> items = null;
            if (this.player != null) {
                try {
                    items = HandlerToggleEnchantments.itemList(this.player);
                } catch (Throwable t) {
                    items = null;
                }
            }
            int n = (items == null) ? 6 : Math.max(6, items.size());
            if (this.chooseItem == null || this.chooseItem.length < n) {
                this.chooseItem = new ButtonWidget[Math.max(64, n + 8)];
            }
            this.imageHeight = 166 + Math.max(0, n - 6) * 18;
        } catch (Throwable t) {
            // fail-open: pantalla vainilla intacta
        }
    }
}
