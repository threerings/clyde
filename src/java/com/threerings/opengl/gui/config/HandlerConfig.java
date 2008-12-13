//
// $Id$

package com.threerings.opengl.gui.config;

import com.threerings.editor.Editable;
import com.threerings.editor.EditorTypes;
import com.threerings.export.Exportable;
import com.threerings.expr.Function;
import com.threerings.expr.Scope;
import com.threerings.expr.util.ScopeUtil;
import com.threerings.util.DeepObject;

import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.ActionListener;
import com.threerings.opengl.gui.event.ChangeEvent;
import com.threerings.opengl.gui.event.ChangeListener;
import com.threerings.opengl.gui.event.ComponentListener;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.KeyListener;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.event.MouseListener;
import com.threerings.opengl.gui.event.MouseMotionListener;
import com.threerings.opengl.gui.event.MouseWheelListener;
import com.threerings.opengl.gui.event.TextEvent;
import com.threerings.opengl.gui.event.TextListener;

/**
 * Contains an event handler configuration.
 */
@EditorTypes({
    HandlerConfig.Action.class, HandlerConfig.Change.class,
    HandlerConfig.Key.class, HandlerConfig.Mouse.class,
    HandlerConfig.MouseMotion.class, HandlerConfig.MouseWheel.class,
    HandlerConfig.Text.class })
public abstract class HandlerConfig extends DeepObject
    implements Exportable
{
    /**
     * Marker interface for handler listeners.
     */
    public interface Listener extends ComponentListener
    {
    }

    /**
     * Handles an action event.
     */
    public static class Action extends HandlerConfig
    {
        /** The name of the function to call. */
        @Editable
        public String function = "actionPerformed";

        @Override // documentation inherited
        public Listener createListener (Scope scope)
        {
            final Function fn = ScopeUtil.resolve(scope, function, Function.NULL);
            class ListenerImpl implements ActionListener, Listener {
                public void actionPerformed (ActionEvent event) {
                    fn.call(event);
                }
            };
            return new ListenerImpl();
        }
    }

    /**
     * Handles a change event.
     */
    public static class Change extends HandlerConfig
    {
        /** The name of the function to call. */
        @Editable
        public String function = "stateChanged";

        @Override // documentation inherited
        public Listener createListener (Scope scope)
        {
            final Function fn = ScopeUtil.resolve(scope, function, Function.NULL);
            class ListenerImpl implements ChangeListener, Listener {
                public void stateChanged (ChangeEvent event) {
                    fn.call(event);
                }
            };
            return new ListenerImpl();
        }
    }

    /**
     * Handles a key event.
     */
    public static class Key extends HandlerConfig
    {
        /** The name of the function to call when a key is pressed. */
        @Editable
        public String pressFunction = "keyPressed";

        /** The name of the function to call when a key is released. */
        @Editable
        public String releaseFunction = "keyReleased";

        @Override // documentation inherited
        public Listener createListener (Scope scope)
        {
            final Function press = ScopeUtil.resolve(scope, pressFunction, Function.NULL);
            final Function release = ScopeUtil.resolve(scope, releaseFunction, Function.NULL);
            class ListenerImpl implements KeyListener, Listener {
                public void keyPressed (KeyEvent event) {
                    press.call(event);
                }
                public void keyReleased (KeyEvent event) {
                    release.call(event);
                }
            };
            return new ListenerImpl();
        }
    }

    /**
     * Handles a mouse event.
     */
    public static class Mouse extends HandlerConfig
    {
        /** The function to call when a mouse button is pressed. */
        @Editable
        public String pressFunction = "mousePressed";

        /** The function to call when a mouse button is released. */
        @Editable
        public String releaseFunction = "mouseReleased";

        /** The function to call when a mouse button is clicked. */
        @Editable
        public String clickFunction = "mouseClicked";

        /** The function to call when the mouse enters the component. */
        @Editable
        public String enterFunction = "mouseEntered";

        /** The function to call when the mouse exits the component. */
        @Editable
        public String exitFunction = "mouseExited";

        @Override // documentation inherited
        public Listener createListener (Scope scope)
        {
            final Function press = ScopeUtil.resolve(scope, pressFunction, Function.NULL);
            final Function release = ScopeUtil.resolve(scope, releaseFunction, Function.NULL);
            final Function click = ScopeUtil.resolve(scope, clickFunction, Function.NULL);
            final Function enter = ScopeUtil.resolve(scope, enterFunction, Function.NULL);
            final Function exit = ScopeUtil.resolve(scope, exitFunction, Function.NULL);
            class ListenerImpl implements MouseListener, Listener {
                public void mousePressed (MouseEvent event) {
                    press.call(event);
                }
                public void mouseReleased (MouseEvent event) {
                    release.call(event);
                }
                public void mouseClicked (MouseEvent event) {
                    click.call(event);
                }
                public void mouseEntered (MouseEvent event) {
                    enter.call(event);
                }
                public void mouseExited (MouseEvent event) {
                    exit.call(event);
                }
            };
            return new ListenerImpl();
        }
    }

    /**
     * Handles a mouse motion event.
     */
    public static class MouseMotion extends HandlerConfig
    {
        /** The function to call when the mouse is moved. */
        @Editable
        public String moveFunction = "mouseMoved";

        /** The function to call when the mouse is dragged. */
        @Editable
        public String dragFunction = "mouseDragged";

        @Override // documentation inherited
        public Listener createListener (Scope scope)
        {
            final Function move = ScopeUtil.resolve(scope, moveFunction, Function.NULL);
            final Function drag = ScopeUtil.resolve(scope, dragFunction, Function.NULL);
            class ListenerImpl implements MouseMotionListener, Listener {
                public void mouseMoved (MouseEvent event) {
                    move.call(event);
                }
                public void mouseDragged (MouseEvent event) {
                    drag.call(event);
                }
            };
            return new ListenerImpl();
        }
    }

    /**
     * Handles a mouse wheel event.
     */
    public static class MouseWheel extends HandlerConfig
    {
        /** The name of the function to call. */
        @Editable
        public String function = "mouseWheeled";

        @Override // documentation inherited
        public Listener createListener (Scope scope)
        {
            final Function fn = ScopeUtil.resolve(scope, function, Function.NULL);
            class ListenerImpl implements MouseWheelListener, Listener {
                public void mouseWheeled (MouseEvent event) {
                    fn.call(event);
                }
            };
            return new ListenerImpl();
        }
    }

    /**
     * Handles a text event.
     */
    public static class Text extends HandlerConfig
    {
        /** The name of the function to call. */
        @Editable
        public String function = "textChanged";

        @Override // documentation inherited
        public Listener createListener (Scope scope)
        {
            final Function fn = ScopeUtil.resolve(scope, function, Function.NULL);
            class ListenerImpl implements TextListener, Listener {
                public void textChanged (TextEvent event) {
                    fn.call(event);
                }
            };
            return new ListenerImpl();
        }
    }

    /**
     * Creates a listener for this handler.
     */
    public abstract Listener createListener (Scope scope);
}
