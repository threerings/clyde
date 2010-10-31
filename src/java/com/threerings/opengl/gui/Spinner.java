//
// $Id$

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
import com.threerings.opengl.gui.layout.HGroupLayout;
import com.threerings.opengl.gui.layout.VGroupLayout;

/**
 * Displays a value with little next and previous buttons.
 */
// TODO: This should be a Component, since we don't want to expose add(), etc.
//       Create a CompoundComponent that extends Component but has a Container inside it
//       so that it can add its own subcomponents, privately.
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
        super(ctx, new HGroupLayout());
        _model = model;

        // create our subcomponents
        _editor = createEditor(model);
        _next = new Button(ctx, "+", "next");
        _next.setStyleConfig(getDefaultStyleConfig() + "Next");
        _next.addListener(_buttonListener);
        _prev = new Button(ctx, "-", "prev");
        _prev.setStyleConfig(getDefaultStyleConfig() + "Prev");
        _prev.addListener(_buttonListener);

        Container buttons = new Container(ctx, new VGroupLayout());
        buttons.add(_next);
        buttons.add(_prev);
        add(_editor);
        add(buttons);
    }

    /**
     * Set a new model.
     */
    public void setModel (SpinnerModel newModel)
    {
        if (!newModel.equals(_model)) { // will NPE if newModel is null
            SpinnerModel oldModel = _model;
            _model = newModel;
            if (isAdded()) {
                oldModel.removeChangeListener(_modelListener);
                newModel.addChangeListener(_modelListener);
                valueChanged();
            }
        }
    }

    /**
     * Get the current model.
     */
    public SpinnerModel getModel ()
    {
        return _model;
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
    public void wasAdded ()
    {
        super.wasAdded();

        _model.addChangeListener(_modelListener);
        valueChanged();
    }

    @Override
    public void wasRemoved ()
    {
        super.wasRemoved();

        _model.removeChangeListener(_modelListener);
    }

    @Override
    public void setStyleConfig (String name)
    {
        super.setStyleConfig(name);
        _editor.setStyleConfig(name);
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
     * The state of the model has changed: update our subcomponents and fire an action
     * if applicable.
     */
    protected void valueChanged ()
    {
        _next.setEnabled(null != _model.getNextValue());
        _prev.setEnabled(null != _model.getPreviousValue());
        _editor.setText(String.valueOf(_model.getValue()));

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
        emitEvent(new ActionEvent(this, when, modifiers, _action, _model.getValue()));
    }

    @Override
    protected String getDefaultStyleConfig ()
    {
        return "Default/Spinner";
    }

    @Override
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);
    }

    /** Our model. */
    protected SpinnerModel _model;

    /** Our action string. */
    protected String _action;

    /** The next and previous buttons. */
    protected Button _next, _prev;

    /** Displays and allows direct editing of the value. */
    protected TextComponent _editor;

    /** Listens for changes to the model and updates our state. */
    protected ChangeListener _modelListener = new ChangeListener() {
        public void stateChanged (ChangeEvent e) {
            valueChanged();
        }
    };

    /** Listens to our buttons and updates the model when they're pressed. */
    protected ActionListener _buttonListener = new ActionListener() {
        public void actionPerformed (ActionEvent e) {
            _model.setValue(
                "next".equals(e.getAction()) ? _model.getNextValue() : _model.getPreviousValue());
        }
    };
}
