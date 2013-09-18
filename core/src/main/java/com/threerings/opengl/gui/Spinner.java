//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2012 Three Rings Design, Inc.
// http://code.google.com/p/clyde/
//
// Redistribution and use in source and binary forms, with or without modification, are permitted
// provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice, this list of
//    conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright notice, this list of
//    conditions and the following disclaimer in the documentation and/or other materials provided
//    with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
// INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
// PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
// INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
// TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package com.threerings.opengl.gui;

import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.threerings.config.ConfigReference;

import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.layout.GroupLayout;

/**
 * Displays a value with little next and previous buttons.
 */
// TODO: This should be a Component, since we don't want to expose add(), etc.
//       Create a CompoundComponent that extends Component but has a Container inside it
//       so that it can add its own subcomponents, privately.
//       (Maybe: Container extends CompoundComponent and just makes all the methods public)
public class Spinner extends Container
    implements UIConstants
{
    /**
     * Creates a spinner with a default Number spinner model with no minimum or maximum value,
     * stepSize equal to 1, and an initial value of 0.
     */
    public Spinner (GlContext ctx)
    {
        this(ctx, new SpinnerNumberModel());
    }

    /**
     * Creates a spinner with the specified spinner model.
     */
    public Spinner (GlContext ctx, SpinnerModel model)
    {
        super(ctx);

        // create our subcomponents
        _editor = createEditor(model);
        _next = new Button(ctx, "+", "next");
        _next.setStyleConfig(getDefaultStyleConfig() + "Next");
        _prev = new Button(ctx, "-", "prev");
        _prev.setStyleConfig(getDefaultStyleConfig() + "Prev");

        _logic = new SpinnerLogic(_editor, _next, _prev, model);
        model.addChangeListener(_modelListener);

        // lay everything out
        setLayoutManager(GroupLayout.makeHoriz(
            GroupLayout.STRETCH, GroupLayout.RIGHT, GroupLayout.EQUALIZE));
        add(_editor);

        // TODO: have the gap be settable in the style
        GroupLayout butLayout = GroupLayout.makeVert(GroupLayout.CENTER).setGap(0);
        Container butBox = new Container(ctx, butLayout);
        butBox.add(_next);
        butBox.add(_prev);
        add(butBox, GroupLayout.FIXED);
    }

    /**
     * Set a new model.
     */
    public void setModel (SpinnerModel newModel)
    {
        SpinnerModel oldModel = _logic.getModel();
        _logic.setModel(newModel);
        oldModel.removeChangeListener(_modelListener);
        newModel.addChangeListener(_modelListener);
        valueChanged();
    }

    /**
     * Get the current model.
     */
    public SpinnerModel getModel ()
    {
        return _logic.getModel();
    }

    /**
     * Set the action that will be fired when the state of this spinner changes, the argument
     * will be the current value of the model.
     */
    public void setAction (String action)
    {
        _action = action;
    }

    /**
     * Get the action of this spinner.
     */
    public String getAction ()
    {
        return _action;
    }

    @Override
    public void setEnabled (boolean enabled)
    {
        // TODO: since we are a Container, when we're enabled all our children are enabled as well.
        // We need to ensure that our buttons are disabled when they should be
        super.setEnabled(enabled);
        _logic.setEnabled(enabled);
    }

    @Override
    public void setStyleConfig (String name)
    {
        super.setStyleConfig(name);
        _editor.setStyleConfig(name + "Editor");
        _next.setStyleConfig(name + "Next");
        _prev.setStyleConfig(name + "Prev");
    }

    /**
     * Configures the style of the editor.
     */
    public void setEditorStyleConfig (ConfigReference<StyleConfig> ref)
    {
        _editor.setStyleConfig(ref);
    }

    /**
     * Configures the style of the next button.
     */
    public void setNextStyleConfig (ConfigReference<StyleConfig> ref)
    {
        _next.setStyleConfig(ref);
    }

    /**
     * Configures the style of the previous button.
     */
    public void setPreviousStyleConfig (ConfigReference<StyleConfig> ref)
    {
        _prev.setStyleConfig(ref);
    }

    /**
     * Create the editor to use for this spinner.
     */
    protected TextComponent createEditor (SpinnerModel model)
    {
        // TODO: allow editable editors!
        return new Label(_ctx, "");
    }

    /**
     * The state of the model has changed: fire an action if applicable.
     */
    protected void valueChanged ()
    {
        if (isAdded()) {
            Root root = getWindow().getRoot();
            fireAction(root.getTickStamp(), root.getModifiers());
        }
    }

    /**
     * Fire our action.
     */
    protected void fireAction (long when, int modifiers)
    {
        emitEvent(new ActionEvent(this, when, modifiers, _action, _logic.getValue()));
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/Spinner";
    }

    /** Spinner logic. */
    protected SpinnerLogic _logic;

    /** Our action string. */
    protected String _action;

    /** Our label. */
    protected TextComponent _editor;

    /** Our buttons. */
    protected Button _next, _prev;

    /** Listens for changes to the model and updates our state. */
    protected ChangeListener _modelListener = new ChangeListener() {
        public void stateChanged (ChangeEvent e) {
            valueChanged();
        }
    };
}
