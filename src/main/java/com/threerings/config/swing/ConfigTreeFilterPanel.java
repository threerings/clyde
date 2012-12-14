//
// $Id$

package com.threerings.config.swing;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.google.common.base.Predicate;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.event.DocumentAdapter;

import com.threerings.util.MessageManager;

import com.threerings.config.ManagedConfig;

/**
 * Displays a panel with a box for filtering configs in an associated ConfigTree.
 */
public class ConfigTreeFilterPanel extends JPanel
{
    /**
     * Create a ConfigTreeFilterPanel.
     */
    public ConfigTreeFilterPanel (MessageManager msgmgr)
    {
        super(new HGroupLayout(HGroupLayout.STRETCH));

        _input = new JTextField();
        _input.getDocument().addDocumentListener(_inputListener);

        add(new JLabel(msgmgr.getBundle("config").get("l.filter_config")), HGroupLayout.FIXED);
        add(_input);
    }

    /**
     * Clear the current filter, if not already.
     */
    public void clearFilter ()
    {
        _input.setText("");
    }

    /**
     * Set the tree for which we're operating.
     */
    public void setTree (ConfigTree tree)
    {
        // Thought: should we reset the filter on the old tree? Right now: no
//        if (_tree != null) {
//            _tree.setFilter(null);
//        }
        _tree = tree;
        setFilter();
    }

    /**
     * Set our filter on the tree.
     */
    protected void setFilter ()
    {
        if (_tree != null) {
            _tree.setFilter(_filter);
        }
    }

    /** The config tree. */
    protected ConfigTree _tree;

    /** The input field. */
    protected JTextField _input;

    /** The actual filter we're configured with. */
    protected Predicate<ManagedConfig> _filter = null;

    /** Listens for changes on the filter document. */
    protected DocumentAdapter _inputListener = new DocumentAdapter() {
        @Override
        public void documentChanged ()
        {
            final String text = _input.getText().trim().toLowerCase();
            _filter = "".equals(text)
                ? null
                : new Predicate<ManagedConfig>() {
                    public boolean apply (ManagedConfig cfg) {
                        return cfg.getName().toLowerCase().contains(text);
                    }
                };
            setFilter();
        }
    };
}
