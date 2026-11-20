package com.payangar.designsample.vendor.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.payangar.designsample.vendor.config.ModSide;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.Test;

class SideCheckTest {
    @Test
    void onlyAModRequiredOnBothSidesRefusesAClientWithoutIt() {
        assertTrue(SideCheck.refuses(ModSide.BOTH_REQUIRED, false));
        assertFalse(SideCheck.refuses(ModSide.BOTH_REQUIRED, true));
        assertFalse(SideCheck.refuses(ModSide.BOTH_OPTIONAL, false));
        assertFalse(SideCheck.refuses(ModSide.SERVER, false));
        assertFalse(SideCheck.refuses(ModSide.CLIENT, false));
    }

    @Test
    void theMessageNamesTheModAndFallsBackToEnglish() {
        TranslatableContents message =
                (TranslatableContents) SideCheck.missingOnClient("demo", "Demo Mod").getContents();

        assertEquals("demo.vendor.side_check.missing_on_client", message.getKey());
        assertEquals("This server requires the mod %s. Install it to join.", message.getFallback());
        assertEquals("Demo Mod", message.getArgs()[0]);
    }
}
