package com.itsyusei.qsbridge.mixin;

import com.dplayend.togenc.client.ToggleScreen;
import com.dplayend.togenc.util.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only: agranda el arreglo de botones de la pantalla de Toggle
 * Enchantments para las herramientas extra de Inventorio.
 *
 * Solo se toca el arreglo en init(). La altura del fondo y el dibujado
 * NO se modifican por codigo (provocaba crash de texturas): el panel
 * extendido (256px) viene del resource pack activo.
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
        // 256 = altura del panel extendido por resource pack. Sin tocar
        // el render: solo arreglo + altura en init (combinacion que abria).
        try {
            if (this.chooseItem == null || this.chooseItem.length < 64) {
                this.chooseItem = new ButtonWidget[64];
            }
            boolean tools = false;
            if (this.player != null) {
                try {
                    tools = !((de.rubixdev.inventorio.player.inventory.PlayerInventoryExtension)
                            de.rubixdev.inventorio.api.InventorioAPI.getInventoryAddon(
                                    this.player)).toolBelt.stream()
                            .allMatch(net.minecraft.world.item.ItemStack::isEmpty);
                } catch (Throwable t) {
                    tools = false;
                }
            }
            this.imageHeight = tools ? 256 : 166;
        } catch (Throwable t) {
            // fail-open
        }
    }
}
