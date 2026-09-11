package com.itsyusei.qsbridge;

import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bridge: lets QuickShulker resolve shulkers stored in Inventorio's extra
 * rows (DeepPockets, ToolBelt, ...). All hooks are fail-open: if anything
 * is missing or throws, vanilla behavior is preserved.
 */
@Mod(QSBridge.MOD_ID)
public class QSBridge {
    public static final String MOD_ID = "qsbridge";
    public static final Logger LOGGER = LoggerFactory.getLogger("qsbridge");

    public QSBridge() {
        LOGGER.info("QuickShulker-Inventorio bridge active");
    }
}
