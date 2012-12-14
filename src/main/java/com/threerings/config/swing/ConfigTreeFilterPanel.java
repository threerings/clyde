//
// $Id$

package com.threerings.config.swing;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.google.common.base.Predicate;

import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.event.DocumentAdapter;

import com.threerings.config.ManagedConfig;

/**
 * Displays a panel with a box for filtering configs in an associated ConfigTree.
 */
public class ConfigTreeFilterPanel extends JPanel
{
    public ConfigTreeFilterPanel (String label)
    {
        super(new HGroupLayout(HGroupLayout.STRETCH));

        _input = new JTextField();
        _input.getDocument().addDocumentListener(_inputListener);

        add(new JLabel(label), HGroupLayout.FIXED);
        add(_input);
    }

    /**
     * Set the tree for which we're operating.
     */
    public void setTree (ConfigTree tree)
    {
        if (_tree != null) {
            // shit: should we reset the filter on this tree?
            _tree.removeObserver(_treeObserver);
        }
        _tree = tree;
        if (_tree != null) {
            _tree.addObserver(_treeObserver);
        }
        resetFilter();
    }

    /**
     * Set our filter on the tree.
     */
    protected void resetFilter ()
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

    /** Do we want to block changes to the input listener? */
    protected boolean _block;

    /** Listens for changes on the filter document. */
    protected DocumentAdapter _inputListener = new DocumentAdapter() {
        @Override
        public void documentChanged ()
        {
            if (_block) {
                return;
            }
            final String text = _input.getText().trim().toLowerCase();
            _filter = "".equals(text)
                ? null
                : new Predicate<ManagedConfig>() {
                    public boolean apply (ManagedConfig cfg) {
                        return cfg.getName().toLowerCase().contains(text);
                    }
                };
            resetFilter();
        }
    };

    /** Observes our tree. */
    protected ConfigTree.Observer _treeObserver = new ConfigTree.Observer() {
        @Override
        public void filterWasReset ()
        {
            _block = true;
            try {
                _input.setText("");

            } finally {
                _block = false;
            }
        }
    };
}
