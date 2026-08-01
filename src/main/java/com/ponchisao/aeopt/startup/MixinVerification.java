package com.ponchisao.aeopt.startup;

import appeng.crafting.execution.CraftingCpuLogic;
import appeng.helpers.patternprovider.PatternProviderLogic;
import com.ponchisao.aeopt.mixin.CraftingCpuLogicAccessor;
import com.ponchisao.aeopt.mixin.PatternProviderLogicAccessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class MixinVerification {

    private static final Logger LOGGER = LogManager.getLogger("aeopt");

    private MixinVerification() {
    }

    public static void verify() {
        reportAccessor(isCpuAccessorApplied(), "CraftingCpuLogicAccessor",
                "appeng.crafting.execution.CraftingCpuLogic");
        reportAccessor(isProviderAccessorApplied(), "PatternProviderLogicAccessor",
                "appeng.helpers.patternprovider.PatternProviderLogic");
    }

    private static boolean isCpuAccessorApplied() {
        return CraftingCpuLogicAccessor.class.isAssignableFrom(CraftingCpuLogic.class);
    }

    private static boolean isProviderAccessorApplied() {
        return PatternProviderLogicAccessor.class.isAssignableFrom(PatternProviderLogic.class);
    }

    private static void reportAccessor(boolean applied, String accessorName, String targetName) {
        if (applied) {
            LOGGER.info("{} is active on {}", accessorName, targetName);
            return;
        }
        LOGGER.error("{} was not applied to {}. Diagnostics depending on it are disabled. "
                + "Another mod most likely loaded the target class before mixins ran.", accessorName, targetName);
    }
}
