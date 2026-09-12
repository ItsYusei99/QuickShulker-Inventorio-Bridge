package com.itsyusei.qsbridge.mixin;

import com.dplayend.togenc.client.ToggleScreen;
import com.dplayend.togenc.handler.HandlerToggleEnchantments;
import com.dplayend.togenc.util.ButtonWidget;
import com.dplayend.togenc.util.ToggleItem;
import com.itsyusei.qsbridge.QSBridge;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Client-only: grows Toggle Enchantments' screen to fit the extra
 * Inventorio tool entries appended by {@code ToggleItemListMixin}.
 *
 * The vanilla screen allocates exactly 6 item buttons; any more entries
 * crash it with ArrayIndexOutOfBoundsException. This mixin sizes the
 * button array and the background height from the real list size before
 * the screen builds its widgets.
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

    @Shadow
    public java.util.List<ToggleItem> toggleItems;

    @Shadow
    public int imageWidth;

    @Shadow
    public int leftPos;

    @Shadow
    public int topPos;

    @Shadow
    public final net.minecraft.resources.ResourceLocation CONTAINER = null;

    private void qsbridge$fitLayout() {
        try {
            java.util.List<ToggleItem> items = this.toggleItems;
            if (items == null && this.player != null) {
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
            QSBridge.LOGGER.info("[qsbridge] pantalla togenc: items={} imageHeight={}",
                    n, this.imageHeight);
        } catch (Throwable t) {
            // fail-open: pantalla vainilla intacta
        }
    }

    @Inject(method = "init", at = @At("HEAD"), require = 0)
    private void qsbridge$onInitHead(CallbackInfo ci) {
        qsbridge$fitLayout();
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), require = 0)
    private void qsbridge$onRenderBackgroundHead(
            net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        // Garantiza el tamaño en cada frame: init() puede correr antes de
        // que la lista tenga sus entradas finales.
        qsbridge$fitLayout();
    }

    @Inject(method = "renderBackground", at = @At("TAIL"), require = 0)
    private void qsbridge$onRenderBackgroundTail(
            net.minecraft.client.gui.GuiGraphics guiGraphics, int mouseX, int mouseY,
            float partialTick, CallbackInfo ci) {
        // La textura del panel mide 176x166 dentro de un archivo de 256x256:
        // estirar el blit NO mueve el borde visible (siempre queda en y=166).
        // Se rellena la extension con el color de fondo y se redibuja el
        // borde inferior con la franja original de la textura.
        try {
            int n = (this.toggleItems == null) ? 6 : Math.max(6, this.toggleItems.size());
            int extra = Math.max(0, n - 6) * 18;
            if (extra <= 0) {
                return;
            }
            int x0 = this.leftPos;
            int y166 = this.topPos + 166;
            int yEnd = this.topPos + this.imageHeight;
            // Fondo interior (mismo tono oliva del panel).
            guiGraphics.fill(x0, y166, x0 + this.imageWidth, yEnd, 0xFF544C3B);
            // Bordes laterales negros de 1px.
            guiGraphics.fill(x0, y166, x0 + 1, yEnd, 0xFF000000);
            guiGraphics.fill(x0 + this.imageWidth - 1, y166, x0 + this.imageWidth, yEnd,
                    0xFF000000);
            // Franja del borde inferior original (6px altos de la textura).
            guiGraphics.blit(this.CONTAINER, x0, yEnd - 6, 0.0F, 160.0F,
                    this.imageWidth, 6, 256, 256);
        } catch (Throwable t) {
            // fail-open
        }
    }
}
