//
// $Id$

package com.threerings.editor.swing.editors;

import javax.swing.JOptionPane;

import com.threerings.config.ConfigGroup;
import com.threerings.config.ConfigReference;
import com.threerings.config.DerivedConfig;
import com.threerings.config.ManagedConfig;
import static com.threerings.editor.Log.log;

/**
 * An editor for the configuration references in a DerivedConfig.
 */
public class DerivedConfigReferenceEditor extends ConfigReferenceEditor
{
    @Override
    protected Class<?> getArgumentType ()
    {
    	if (_object != null && _object instanceof DerivedConfig) {
    		return ((DerivedConfig)_object).cclass;
    	} else {
    		log.warning("DerivedConfigReferenceEditor - missing or bad object type", "object", _object);
    		return null;
    	}
    }

    @Override
    protected boolean validateNewValue (ConfigReference<?> value)
    {
        DerivedConfig der = (DerivedConfig)_object;
        ConfigGroup<ManagedConfig> group = der.getConfigManager().getGroup(der);
        String name = der.getName();
        for (String n = value.getName(); true; ) {
            if (name.equals(n)) {
                JOptionPane.showMessageDialog(this, _msgs.get("e.derived_cfg_loop"));
                return false;
            }
            ManagedConfig refValue = group.getRawConfig(n);
            if (!(refValue instanceof DerivedConfig)) {
                break;
            }
            DerivedConfig derValue = (DerivedConfig)refValue;
            if (derValue.base == null) {
                break;
            }
            n = derValue.base.getName();
        }
        return true;
    }
}
