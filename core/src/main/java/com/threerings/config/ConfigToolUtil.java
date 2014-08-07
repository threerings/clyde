//
// $Id$

package com.threerings.config;

/**
 * Super-hacky utility methods so that tools in a different package can
 * access protected members of this package.
 * I should probably just make the method public but that's a big change and
 * clutters the public API of ManagedConfig.
 */
public class ConfigToolUtil
{
    /**
     * Get update references for the specified config.
     */
    public static void getUpdateReferences (ManagedConfig cfg, ConfigReferenceSet refSet)
    {
        cfg.getUpdateReferences(refSet);
    }
}
