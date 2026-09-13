package com.itsyusei.sophisquicktorio.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Resolución de inventarios para el provider de WTHIT.
 * Dos fixes sobre el original:
 * 1. Condición de refresco corregida (upstream usa && en vez de || y nunca
 *    cambia de bloque en el mismo mundo -> preview pegado).
 * 2. Fallback por lados: varios BEs (Sophisticated, Create) solo exponen su
 *    item handler en lados concretos; con contexto null devuelven null y el
 *    preview sale vacío. Se prueba null y luego cada Direction.
 * La búsqueda por lados es directa (sin caché, sin listeners) para no fugar.
 */
public final class WthitCacheHolder {
    private static BlockCapabilityCache<IItemHandler, Direction> cache;

    private WthitCacheHolder() {
    }

    public static synchronized IItemHandler getHandler(ServerLevel world, BlockPos pos) {
        if (cache == null || cache.level() != world || !cache.pos().equals(pos)) {
            cache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, world, pos, null);
        }
        IItemHandler handler = null;
        try {
            handler = cache.getCapability();
        } catch (Throwable ignored) {
        }
        if (handler != null) {
            return handler;
        }
        for (Direction side : Direction.values()) {
            try {
                handler = world.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            } catch (Throwable ignored) {
                handler = null;
            }
            if (handler != null) {
                return handler;
            }
        }
        return null;
    }
}
