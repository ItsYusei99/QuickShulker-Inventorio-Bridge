package com.itsyusei.sophisquicktorio;

import com.itsyusei.sophisquicktorio.soph.OpenSophShulkerPayload;
import com.itsyusei.sophisquicktorio.soph.SophMenus;
import com.itsyusei.sophisquicktorio.soph.SophShulker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge: lets QuickShulker resolve shulkers stored in Inventorio's extra
 * rows (DeepPockets, ToolBelt, ...), plus portable opening of Sophisticated
 * Storage shulkers (which QuickShulker itself ignores).
 * All hooks are fail-open: if anything is missing or throws, vanilla
 * behavior is preserved.
 */
@Mod(SophisQuickTorio.MOD_ID)
public class SophisQuickTorio {
    public static final String MOD_ID = "sophisquicktorio";
    public static final Logger LOGGER = LoggerFactory.getLogger("sophisquicktorio");

    public SophisQuickTorio(IEventBus modBus) {
        LOGGER.info("QuickShulker-Inventorio bridge active");
        try {
            if (ModList.get() != null && ModList.get().isLoaded(SophShulker.SOPH_MOD_ID)) {
                SophMenus.MENUS.register(modBus);
                modBus.addListener(SophisQuickTorio::registerPayloads);
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    com.itsyusei.sophisquicktorio.soph.SophShulkerClient.init(modBus);
                }
                LOGGER.info("[sqt] Soph Storage shulker support active");
            }
        } catch (Throwable t) {
            LOGGER.warn("[sqt] Soph support disabled: {}", t.toString());
        }
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        try {
            event.registrar("1").playToServer(
                    OpenSophShulkerPayload.TYPE,
                    OpenSophShulkerPayload.CODEC,
                    OpenSophShulkerPayload::handleOnNet);
        } catch (Throwable t) {
            LOGGER.warn("[sqt] payload registration failed: {}", t.toString());
        }
    }
}
