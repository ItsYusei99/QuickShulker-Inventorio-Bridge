package com.itsyusei.sophisquicktorio.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Caché propia con la condición corregida para el provider de WTHIT.
 * Upstream usa && donde debe usar || y nunca refresca al cambiar de bloque.
 */
public final class WthitCacheHolder {
    private static BlockCapabilityCache<IItemHandler, Direction> cache;

    private WthitCacheHolder() {
    }

    public static synchronized BlockCapabilityCache<IItemHandler, Direction> get(
            ServerLevel world, BlockPos pos) {
        if (cache == null || cache.level() != world || !cache.pos().equals(pos)) {
            cache = BlockCapabilityCache.create(Capabilities.ItemHandler.BLOCK, world, pos, null);
        }
        return cache;
    }
}
