//
// $Id$

package com.threerings.config;

/**
 * Super-hacky utility methods so that tools in a different package can
 * access protected members of this package.
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
