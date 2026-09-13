package com.itsyusei.qsbridge.mixin;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client-only: evita el conteo doble en los previews de contenedores de
 * WTHIT (y bloques compatibles como Sophisticated Storage o Stock Keepers).
 *
 * WTHIT dibuja por cada celda DOS textos superpuestos: el conteo completo
 * vainilla (ej. "6400", que desborda la celda) mas su version compacta
 * (ej. "6.4K"). Con stacks gigantes de Sophisticated Storage el resultado
 * es ilegible. Este mixin omite el dibujado vainilla cuando el conteo
 * supera 999 y deja solo la version compacta escalada de WTHIT.
 *
 * Fail-open (require = 0 + try/catch).
 */
@Mixin(value = mcp.mobius.waila.api.component.ItemComponent.class, remap = false)
public class WthitItemDecoratorMixin {

    @Redirect(
            method = "renderItemDecorations",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V"
            ),
            require = 0
    )
    private static void qsbridge$skipVanillaCount(GuiGraphics graphics, Font font,
            ItemStack stack, int x, int y, String text) {
        try {
            if (stack != null && stack.getCount() > 999) {
                // WTHIT ya dibuja "6.4K" compacto y escalado justo despues.
                return;
            }
        } catch (Throwable t) {
            // fail-open: dibujar normal
        }
        try {
            graphics.renderItemDecorations(font, stack, x, y, text);
        } catch (Throwable t) {
            // fail-open silencioso
        }
    }
}
