package com.itsyusei.sophisquicktorio.mixin;

import com.itsyusei.sophisquicktorio.util.WthitCacheHolder;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.data.ItemData;
import mcp.mobius.waila.plugin.neo.provider.ItemCapabilityProvider;import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side: corrige el preview pegado de WTHIT en bloques con inventario
 * (cofres Sophisticated, máquinas de Create...).
 *
 * El provider original guarda UN solo BlockCapabilityCache en un singleton y
 * solo lo recrea con {@code cache.level() != world && !cache.pos().equals(pos)}
 * (&& en vez de ||): en el mismo mundo nunca se refresca al mirar otro bloque
 * y todos muestran el inventario del primero.
 *
 * Se replica su lógica con la condición corregida (ver {@link WthitCacheHolder})
 * y se cancela el original. Fail-open (require = 0 + try/catch): si WTHIT
 * cambia o falta, se ejecuta el original sin romper nada.
 */
@Mixin(value = ItemCapabilityProvider.class, remap = false)
public class WthitCapabilityCacheMixin {
    private static boolean sqtLogged = false;
    private static String sqtLastPos = "";
    private static int sqtWarns = 0;

    @Inject(method = "appendData", at = @At("HEAD"), cancellable = true, require = 0)
    private void sqt$fixStaleCache(IDataWriter data, IServerAccessor<BlockEntity> accessor,
            IPluginConfig config, CallbackInfo ci) {
        try {
            if (!sqtLogged) {
                sqtLogged = true;
                com.itsyusei.sophisquicktorio.SophisQuickTorio.LOGGER
                        .info("[sqt] WTHIT capability fix active");
            }
            var world = accessor.getLevel();
            var pos = accessor.getTarget().getBlockPos();
            var handler = WthitCacheHolder.getHandler(world, pos);
            String key = pos.toShortString();
            if (!key.equals(sqtLastPos)) {
                sqtLastPos = key;
                int slots = -1;
                String first = "?";
                try {
                    if (handler != null) {
                        slots = handler.getSlots();
                        first = String.valueOf(handler.getStackInSlot(0).getItem());
                    }
                } catch (Throwable ignored) {
                }
                com.itsyusei.sophisquicktorio.SophisQuickTorio.LOGGER.info(
                        "[sqt] wthit target {} slots={} first={} null={}",
                        key, slots, first, handler == null);
            }
            if (handler == null) {
                ci.cancel();
                return;
            }
            data.add(ItemData.TYPE,
                    res -> res.add(ItemData.of(config).getter(handler::getStackInSlot, handler.getSlots())));
            ci.cancel();
        } catch (Throwable t) {
            // fail-open: dejar que corra el original (pero registrando la causa, máx 3)
            if (sqtWarns < 3) {
                sqtWarns++;
                com.itsyusei.sophisquicktorio.SophisQuickTorio.LOGGER.warn(
                        "[sqt] wthit fix skipped append", t);
            }
        }
    }
}
