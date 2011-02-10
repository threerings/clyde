//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2011 Three Rings Design, Inc.
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

import javax.swing.undo.UndoManager;

import org.lwjgl.opengl.GL11;

import com.samskivert.util.IntTuple;

import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.config.CursorConfig;
import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.FocusEvent;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.event.TextEvent;
import com.threerings.opengl.gui.text.DefaultKeyMap;
import com.threerings.opengl.gui.text.KeyMap;
import com.threerings.opengl.gui.text.Document;
import com.threerings.opengl.gui.text.EditCommands;
import com.threerings.opengl.gui.text.LengthLimitedDocument;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Extends TextComponent with mechanisms shared by editable text Components.
 */
public abstract class EditableTextComponent extends TextComponent
    implements EditCommands, Document.Listener
{
    /**
     * For subclasses.
     */
    protected EditableTextComponent (GlContext ctx)
    {
        this(ctx, "", 0);
    }

    /**
     * For subclasses.
     */
    protected EditableTextComponent (GlContext ctx, String text, int maxLength)
    {
        super(ctx);
        setMaxLength(maxLength);
        setText(text);
    }

    /**
     * Configures this text field with the specified text for display and
     * editing. The cursor will be adjusted if this text is shorter than
     * its previous position.
     */
    public void setText (String text)
    {
        if (text == null) {
            text = "";
        }
        if (!_text.getText().equals(text)) {
            _text.setText(text, -1);
            _undomgr.discardAllEdits();
        }
    }

    // documentation inherited
    public String getText ()
    {
        return _text.getText();
    }

    /**
     * Configures the maximum length of this text field. This will replace
     * any currently set document with a LengthLimitedDocument (or no document
     * at all if maxLength is <= 0).
     */
    public void setMaxLength (int maxLength)
    {
        if (maxLength > 0) {
            setDocument(new LengthLimitedDocument(maxLength));
        } else {
            setDocument(new Document());
        }
    }

    /**
     * Configures this text field with a custom document.
     */
    public void setDocument (Document document)
    {
        if (_text != null) {
            _text.removeListener(this);
            _text.removeUndoableEditListener(_undomgr);
        }
        _text = document;
        _text.addListener(this);
        _text.addUndoableEditListener(_undomgr);
    }

    /**
     * Returns the underlying document used by this text field to maintain its
     * state. Changes to the document will be reflected in the text field
     * display.
     */
    public Document getDocument ()
    {
        return _text;
    }

    /**
     * Configures the preferred width of this text field (the preferred
     * height will be calculated from the font).
     */
    public void setPreferredWidth (int width)
    {
        _prefWidth = width;
    }

    /**
     * Returns the selection background configured for this component.
     */
    public Background getSelectionBackground ()
    {
        Background background = _selectionBackgrounds[getState()];
        return (background != null) ? background : _selectionBackgrounds[DEFAULT];
    }

    // documentation inherited from interface Document.Listener
    public void textInserted (Document document, int offset, int length)
    {
        // put the cursor at the end of the insertion
        setCursorPos(offset + length);

        // if we're already part of the hierarchy, recreate our glyps
        if (isAdded()) {
            recreateGlyphs();
        }

        // let anyone who is around to hear know that a tree fell in the woods
        emitEvent(new TextEvent(this, -1L));
    }

    // documentation inherited from interface Document.Listener
    public void textRemoved (Document document, int offset, int length)
    {
        // put the cursor at the beginning of the deletion
        setCursorPos(offset);

        // if we're already part of the hierarchy, recreate our glyps
        if (isAdded()) {
            recreateGlyphs();
        }

        // let anyone who is around to hear know that a tree fell in the woods
        emitEvent(new TextEvent(this, -1L));
    }

    // documentation inherited
    public boolean acceptsFocus ()
    {
        return isVisible() && isEnabled();
    }

    // documentation inherited
    public boolean dispatchEvent (Event event)
    {
        if (event instanceof KeyEvent) {
            KeyEvent kev = (KeyEvent)event;
            if (kev.getType() == KeyEvent.KEY_PRESSED) {
                int modifiers = kev.getModifiers();
                int cmd = _keymap.lookupMapping(modifiers, kev.getKeyCode());
                switch (cmd) {
                case KeyMap.NO_MAPPING:
                    char c = kev.getKeyChar();
                    // if otherwise unprocessed, insert printable and shifted/alted printable chars
                    if ((modifiers & ~(KeyEvent.SHIFT_DOWN_MASK | KeyEvent.ALT_DOWN_MASK)) == 0 &&
                            Character.isDefined(c) && ((c == '\n') || !Character.isISOControl(c))) {
                        replaceSelectedText(String.valueOf(c),
                            Character.isLetterOrDigit(c) ?
                                CompoundType.WORD_CHAR : CompoundType.NONWORD_CHAR);
                        return true;
                    }
                    break;

                case ACTION:
                    emitEvent(new ActionEvent(this, event.getWhen(), modifiers, "", getText()));
                    return true;

                default:
                    if (processCommand(cmd)) {
                        return true;
                    }
                    break;
                }
            }

        } else if (event instanceof MouseEvent &&
                // don't adjust the cursor/selection if we have no text
                _text.getLength() > 0 && hasGlyphs()) {
            MouseEvent mev = (MouseEvent)event;
            Insets insets = getInsets();
            int mx = mev.getX() - getAbsoluteX() - insets.left,
                my = mev.getY() - getAbsoluteY() - insets.bottom;
            int pos = getPosition(mx, my);
            int type = mev.getType();
            if (type == MouseEvent.MOUSE_PRESSED) {
                // if pressed inside the selection, wait for click
                if (!selectionContains(pos)) {
                    setCursorPos(pos);
                }
                return true;

            } else if (type == MouseEvent.MOUSE_DRAGGED) {
                setSelection(pos, _selp);
                return true;

            } else if (type == MouseEvent.MOUSE_CLICKED) {
                int count = (mev.getClickCount() - 1) % 3;
                if (count == 0) {
                    setCursorPos(pos);
                } else if (count == 1) {
                    IntTuple extents = _text.getWordExtents(pos);
                    setSelection(extents.left, extents.right);
                } else { // count == 2
                    setSelection(_text.getLength(), 0);
                }
                return true;
            }

        } else if (event instanceof FocusEvent) {
            FocusEvent fev = (FocusEvent)event;
            switch (fev.getType()) {
            case FocusEvent.FOCUS_GAINED:
                gainedFocus();
                break;
            case FocusEvent.FOCUS_LOST:
                lostFocus();
                break;
            }
        }

        return super.dispatchEvent(event);
    }

    /**
     * Process the specified edit command, returning true if it was handled.
     */
    protected boolean processCommand (int cmd)
    {
        switch (cmd) {
        default:
            return false;

        case BACKSPACE:
            if (!selectionIsEmpty()) {
                deleteSelectedText();
            } else if (_cursp > 0 && _text.getLength() > 0) {
                int pos = _cursp-1;
                if (_text.remove(pos, 1, nextUndoId(CompoundType.BACKSPACE))) {
                    setCursorPos(pos);
                    _lastCompoundType = CompoundType.BACKSPACE;
                }
            }
            break;

        case DELETE:
            if (!selectionIsEmpty()) {
                deleteSelectedText();
            } else if (_cursp < _text.getLength()) {
                _text.remove(_cursp, 1, nextUndoId(CompoundType.DELETE));
                _lastCompoundType = CompoundType.DELETE;
            }
            break;

        case CURSOR_LEFT:
            setCursorPos(Math.max(0, _cursp-1));
            break;

        case CURSOR_RIGHT:
            setCursorPos(Math.min(_text.getLength(), _cursp+1));
            break;

        case WORD_LEFT:
            setCursorPos(_text.lastIndexOfWordStart(_cursp));
            break;

        case WORD_RIGHT:
            setCursorPos(_text.indexOfWordEnd(_cursp));
            break;

        case RELEASE_FOCUS:
            getWindow().requestFocus(null);
            break;

        case CLEAR:
            _text.setText("", nextUndoId(null));
            break;

        case CUT:
            if (!selectionIsEmpty()) {
                getWindow().getRoot().setClipboardText(deleteSelectedText());
            }
            break;

        case COPY:
            if (!selectionIsEmpty()) {
                getWindow().getRoot().setClipboardText(getSelectedText());
            }
            break;

        case PASTE:
            String clip = getWindow().getRoot().getClipboardText();
            if (clip != null) {
                clip = validatePaste(clip);
                if (clip != null) {
                    // this works even if nothing is selected
                    replaceSelectedText(clip, null);
                }
            }
            break;

        case UNDO:
            if (_undomgr.canUndo()) {
                _undomgr.undo();
            }
            break;

        case REDO:
            if (_undomgr.canRedo()) {
                _undomgr.redo();
            }
            break;
        }

        return true;
    }

    /**
     * Validate the pasted text. Return null to reject the paste event altogether, or the pasted
     * text may be modified...
     */
    protected String validatePaste (String pasted)
    {
        return pasted;
    }

    @Override // documentation inherited
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

        // utilize the text cursor if none other defined
        if ((state == DEFAULT) && (_cursor == null)) {
            CursorConfig textCursor = _ctx.getConfigManager().getConfig(CursorConfig.class, "Text");
            if (textCursor != null) {
                _cursor = textCursor.getCursor(_ctx);
            }
        }

        // configure our selection background
        _selectionBackgrounds[state] = (config.selectionBackground == null) ?
            null : config.selectionBackground.getBackground(_ctx);

        // TODO: look up our keymap
    }

    // documentation inherited
    protected void wasAdded ()
    {
        super.wasAdded();

        // create our underlying text texture
        recreateGlyphs();
    }

    // documentation inherited
    protected void wasRemoved ()
    {
        super.wasRemoved();

        clearGlyphs();
    }

    // documentation inherited
    protected void layout ()
    {
        super.layout();

        // cope with becoming smaller or larger
        recreateGlyphs();
    }

    // documentation inherited
    protected void stateDidChange ()
    {
        super.stateDidChange();
        recreateGlyphs();
    }

    /**
     * Called when this text field has gained the focus.
     */
    protected void gainedFocus ()
    {
        _showCursor = true;
        setCursorPos(_cursp);
    }

    /**
     * Called when this text field has lost the focus.
     */
    protected void lostFocus ()
    {
        _showCursor = false;
        _undomgr.discardAllEdits();
    }

    /**
     * This method allows a derived class (specifically {@link
     * PasswordField}) to display something other than the actual
     * contents of the text field.
     */
    protected String getDisplayText ()
    {
        return _text.getText();
    }

    /**
     * Helper method to render the cursor.
     */
    protected void renderCursor (Renderer renderer, int x, int y, int height)
    {
        renderer.setColorState(getColor());
        renderer.setTextureState(null);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + height);
        GL11.glEnd();
    }

    /**
     * Checks whether the selection is empty.
     */
    protected boolean selectionIsEmpty ()
    {
        return (_cursp == _selp);
    }

    /**
     * Determines whether the selection contains the specified position.
     */
    protected boolean selectionContains (int pos)
    {
        int start = Math.min(_cursp, _selp), end = Math.max(_cursp, _selp);
        return pos >= start && pos < end;
    }

    /**
     * Returns the currently selected text.
     */
    protected String getSelectedText ()
    {
        int start = Math.min(_cursp, _selp), end = Math.max(_cursp, _selp);
        return _text.getText(start, end - start);
    }

    /**
     * Deletes the currently selected text.
     *
     * @return the previously selected text (an empty string if nothing was selected).
     */
    protected String deleteSelectedText ()
    {
        int start = Math.min(_cursp, _selp), end = Math.max(_cursp, _selp);
        int length = end - start;
        String text = _text.getText(start, length);
        if (_text.remove(start, length, nextUndoId(null))) {
            setCursorPos(start);
        }
        return text;
    }

    /**
     * Replaces the currently selected text with the supplied text.
     */
    protected void replaceSelectedText (String text, CompoundType compoundType)
    {
        int start = Math.min(_cursp, _selp), end = Math.max(_cursp, _selp);
        int length = end - start;
        if (_text.replace(start, length, text, nextUndoId(compoundType))) {
            setCursorPos(start + text.length());
            _lastCompoundType = compoundType;
        }
    }

    /**
     * Updates the cursor position, moving the visible representation as
     * well as the insertion and deletion point.
     */
    protected void setCursorPos (int cursorPos)
    {
        setSelection(cursorPos, cursorPos);
    }

    /**
     * Updates the selection.
     */
    protected void setSelection (int cursorPos, int selectPos)
    {
        // this breaks up any compound edits
        _lastCompoundType = null;

        // note the new selection
        _cursp = cursorPos;
        _selp = selectPos;

        selectionWasSet();
    }

    /**
     * Recreates the entity that we use to render our text.
     */
    protected void recreateGlyphs ()
    {
        clearGlyphs();

        if (_text.getLength() == 0) {
            setSelection(0, 0);

        } else {
            createGlyphs();
            setSelection(_cursp, _selp);
        }
    }

    /**
     * Do we have glyphs computed?
     */
    protected abstract boolean hasGlyphs ();

    /**
     * Clears out our text textures and other related bits.
     */
    protected abstract void clearGlyphs ();

    /**
     * Create the entity that we use to render our text.
     */
    protected abstract void createGlyphs ();

    /**
     * Get the position in our document, given the mouse local mouse coordinates that have
     * already had the insets taken into account.
     */
    protected abstract int getPosition (int mouseX, int mouseY);

    /**
     * Update any internal positions after the selection is set.
     */
    protected abstract void selectionWasSet ();

    /**
     * Returns an undo operation id.
     *
     * @param compoundType identifies the edit type for purposes of compounding events together.
     */
    protected int nextUndoId (CompoundType compoundType)
    {
        // usually, we group edits with the same compound type under the same undo id; however,
        // we have a special case used to group words with their trailing whitespace/punctuation
        if ((compoundType != null && compoundType == _lastCompoundType) ||
                (compoundType == CompoundType.NONWORD_CHAR &&
                    _lastCompoundType == CompoundType.WORD_CHAR)) {
            return _lastUndoId;
        }
        return ++_lastUndoId;
    }

    /** Edits that can be compounded together. */
    protected enum CompoundType { WORD_CHAR, NONWORD_CHAR, BACKSPACE, DELETE };

    protected Document _text;
    protected KeyMap _keymap = new DefaultKeyMap();

    protected int _prefWidth = -1;
    protected boolean _showCursor;
    protected int _cursp, _selp;

    protected Background[] _selectionBackgrounds = new Background[getStateCount()];

    protected Rectangle _srect = new Rectangle();

    protected UndoManager _undomgr = new UndoManager();
    protected int _lastUndoId;
    protected CompoundType _lastCompoundType;
}
