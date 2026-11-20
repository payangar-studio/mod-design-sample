package com.payangar.designsample.neoforge;

import com.payangar.designsample.CommonEntrypoint;
import net.neoforged.fml.common.Mod;

/** NeoForge's entrypoint, on both sides. */
@Mod(CommonEntrypoint.MOD_ID)
public final class NeoForgeEntrypoint {
    public NeoForgeEntrypoint() {
        CommonEntrypoint.init();
    }
}
