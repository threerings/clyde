//
// $Id$

package com.threerings.config.swing;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

import com.samskivert.swing.GroupLayout;
import com.samskivert.swing.HGroupLayout;
import com.samskivert.swing.VGroupLayout;
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
        super(new VGroupLayout());
        ((VGroupLayout)getLayout()).setOffAxisPolicy(GroupLayout.STRETCH);

        _input = new JTextField();
        _input.getDocument().addDocumentListener(_inputListener);

        JPanel box = GroupLayout.makeHBox(HGroupLayout.STRETCH);
        box.add(new JLabel(msgmgr.getBundle("editor.config").get("l.filter_config")),
                HGroupLayout.FIXED);
        box.add(_input);
        box.add(new JButton(_clearAction), HGroupLayout.FIXED);
        add(box);
    }

    /**
     * Add a constraint to this panel.
     */
    public ConfigTreeFilterPanel addConstraint (
            String description, final Predicate<? super ManagedConfig> filter, boolean removable)
    {
        if (_predicates == null) {
            _predicates = Lists.newArrayList();
        }

        _predicates.add(filter);
        final JPanel box = GroupLayout.makeHBox(HGroupLayout.STRETCH);
        final Action clear = new AbstractAction("", UIManager.getIcon("InternalFrame.closeIcon")) {
            @Override
            public void actionPerformed (ActionEvent event) {
                ConfigTreeFilterPanel.this.remove(box);
                _predicates.remove(filter);
                setFilter();
            }
        };
        clear.setEnabled(removable);

        box.add(new JLabel(description));
        box.add(new JButton(clear), HGroupLayout.FIXED);
        add(box, getComponentCount() - 1); // always add it just before the text filter
        return this;
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
            _tree.setFilter(createFilter());
        }
    }

    /**
     * Create the filter to use, which may be null (the tree will use alwaysTrue()).
     */
    protected Predicate<? super ManagedConfig> createFilter ()
    {
        if (_predicates == null) {
            return _filter;
        }
        Predicate<? super ManagedConfig> combined = Predicates.and(_predicates);
        if (_filter != null) {
            combined = Predicates.and(_filter, combined);
        }
        return combined;
    }


    /** The config tree. */
    protected ConfigTree _tree;

    /** The input field. */
    protected JTextField _input;

    /** An action for clearing the filter. */
    protected Action _clearAction = new AbstractAction(
            "", UIManager.getIcon("InternalFrame.closeIcon")) {
        { // initializer
            setEnabled(false);
        }

        @Override
        public void actionPerformed (ActionEvent event) {
            clearFilter();
        }
    };

    /** The actual filter we're configured with. */
    protected Predicate<ManagedConfig> _filter = null;

    /** Additional predicates. */
    protected List<Predicate<? super ManagedConfig>> _predicates;

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
            _clearAction.setEnabled(_filter != null);
        }
    };
}
