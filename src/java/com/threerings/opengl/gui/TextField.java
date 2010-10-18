//
// $Id$
//
// Clyde library - tools for developing networked games
// Copyright (C) 2005-2010 Three Rings Design, Inc.
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

import com.threerings.opengl.renderer.Color4f;
import com.threerings.opengl.renderer.Renderer;
import com.threerings.opengl.util.GlContext;

import com.threerings.opengl.gui.background.Background;
import com.threerings.opengl.gui.config.StyleConfig;
import com.threerings.opengl.gui.event.ActionEvent;
import com.threerings.opengl.gui.event.Event;
import com.threerings.opengl.gui.event.FocusEvent;
import com.threerings.opengl.gui.event.KeyEvent;
import com.threerings.opengl.gui.event.MouseEvent;
import com.threerings.opengl.gui.event.TextEvent;
import com.threerings.opengl.gui.text.DefaultKeyMap;
import com.threerings.opengl.gui.text.KeyMap;
import com.threerings.opengl.gui.text.Text;
import com.threerings.opengl.gui.text.Document;
import com.threerings.opengl.gui.text.EditCommands;
import com.threerings.opengl.gui.text.LengthLimitedDocument;
import com.threerings.opengl.gui.util.Dimension;
import com.threerings.opengl.gui.util.Insets;
import com.threerings.opengl.gui.util.Rectangle;

/**
 * Displays and allows for the editing of a single line of text.
 */
public class TextField extends EditableTextComponent
    implements EditCommands, Document.Listener
{
    /**
     * Creates a blank text field.
     */
    public TextField (GlContext ctx)
    {
        this(ctx, "");
    }

    /**
     * Creates a blank text field with maximum input length.  The maximum input
     * length is controlled by a {@link LengthLimitedDocument}, changing the
     * document will remove the length control.
     */
    public TextField (GlContext ctx, int maxLength)
    {
        this(ctx, "", maxLength);
    }

    /**
     * Creates a text field with the specified starting text.
     */
    public TextField (GlContext ctx, String text)
    {
        this(ctx, text, 0);
    }

    /**
     * Creates a text field with the specified starting text and max length.
     * The maximum input length is controlled by a {@link
     * LengthLimitedDocument}, changing the document will remove the length
     * control.
     */
    public TextField (GlContext ctx, String text, int maxLength)
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
                int modifiers = kev.getModifiers(), keyCode = kev.getKeyCode();
                switch (_keymap.lookupMapping(modifiers, keyCode)) {
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

                case START_OF_LINE:
                    setCursorPos(0);
                    break;

                case END_OF_LINE:
                    setCursorPos(_text.getLength());
                    break;

                case ACTION:
                    emitEvent(new ActionEvent(
                                  this, kev.getWhen(), kev.getModifiers(), "", getText()));
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
                        // this works even if nothing is selected
                        replaceSelectedText(clip, null);
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

                default:
                    // insert printable and shifted printable characters
                    char c = kev.getKeyChar();
                    if ((modifiers & ~KeyEvent.SHIFT_DOWN_MASK) == 0 && Character.isDefined(c) &&
                            !Character.isISOControl(c)) {
                        replaceSelectedText(String.valueOf(c),
                            Character.isLetterOrDigit(c) ?
                                CompoundType.WORD_CHAR : CompoundType.NONWORD_CHAR);
                    } else {
                        return super.dispatchEvent(event);
                    }
                    break;
                }

                return true; // we've consumed these events
            }

        } else if (event instanceof MouseEvent &&
            // don't adjust the cursor/selection if we have no text
            _text.getLength() > 0 && _glyphs != null) {
            MouseEvent mev = (MouseEvent)event;
            Insets insets = getInsets();
            int mx = mev.getX() - getAbsoluteX() - insets.left + _txoff,
                my = mev.getY() - getAbsoluteY() - insets.bottom;
            int pos = _glyphs.getHitPos(mx, my);
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

    @Override // documentation inherited
    protected String getDefaultStyleConfig ()
    {
        return "Default/TextField";
    }

    @Override // documentation inherited
    protected void updateFromStyleConfig (int state, StyleConfig.Original config)
    {
        super.updateFromStyleConfig(state, config);

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
        _glyphs = null;
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

    // documentation inherited
    protected void renderComponent (Renderer renderer)
    {
        super.renderComponent(renderer);

        Insets insets = getInsets();

        // render the selection background if appropriate
        if (_showCursor && _cursx != _selx) {
            Background background = getSelectionBackground();
            if (background != null) {
                int cx = _cursx - _txoff;
                int sx = Math.min(Math.max(_selx - _txoff, 0),
                    _width - insets.getHorizontal() - 1);
                int x1 = Math.min(cx, sx), x2 = Math.max(cx, sx);
                background.render(
                    renderer, insets.left + x1, insets.bottom,
                    x2 - x1 + 1, getTextFactory().getHeight(), _alpha);
            }
        }

        // render our text
        if (_glyphs != null) {
            // clip the text to our visible text region
            Rectangle oscissor = intersectScissor(
                renderer, _srect,
                getAbsoluteX() + insets.left,
                getAbsoluteY() + insets.bottom,
                _width - insets.getHorizontal(),
                _height - insets.getVertical());
            try {
                _glyphs.render(renderer, insets.left - _txoff,
                               insets.bottom, _alpha);
            } finally {
                renderer.setScissor(oscissor);
            }
        }

        // render the cursor if we have focus
        if (_showCursor && _cursx == _selx) {
            int cx = insets.left - _txoff + _cursx;
            renderer.setColorState(getColor());
            renderer.setTextureState(null);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            GL11.glVertex2f(cx, insets.bottom);
            int cheight = getTextFactory().getHeight();
            GL11.glVertex2f(cx, insets.bottom + cheight);
            GL11.glEnd();
        }
    }

    // documentation inherited
    protected Dimension computePreferredSize (int whint, int hhint)
    {
        Dimension d = (_glyphs == null) ?
            new Dimension(0, getTextFactory().getHeight()) :
            new Dimension(_glyphs.getSize());
        if (_prefWidth != -1) {
            d.width = _prefWidth;
        }
        return d;
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
     * Recreates the entity that we use to render our text.
     */
    protected void recreateGlyphs ()
    {
        clearGlyphs();

        // if we have no text, clear out all our internal markers
        if (_text.getLength() == 0) {
            _txoff = _cursp = _cursx = _selp = _selx = 0;
            return;
        }

        // format our text and determine how much of it we can display
        _glyphs = getTextFactory().createText(
            getDisplayText(), getColor(), UIConstants.PLAIN,
            UIConstants.DEFAULT_SIZE, null, true);
        setSelection(_cursp, _selp);
    }

    /**
     * Clears out our text textures and other related bits.
     */
    protected void clearGlyphs ()
    {
        _glyphs = null;
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
     * Checks whether the selection is empty.
     */
    protected boolean selectionIsEmpty ()
    {
        return _cursp == _selp;
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
    protected void setSelection (final int cursorPos, final int selectPos)
    {
        // by default, this breaks up any compound edits
        _lastCompoundType = null;

        // note the new selection
        _cursp = cursorPos;
        _selp = selectPos;

        // compute the new screen positions
        if (_glyphs != null) {
            _cursx = _glyphs.getCursorPos(cursorPos);
            _selx = _glyphs.getCursorPos(selectPos);
        } else {
            _cursx = _selx = 0;
        }

        // scroll our text left or right as necessary
        if (_cursx < _txoff) {
            _txoff = _cursx;
        } else if (_width > 0) { // make sure we're laid out
            int avail = getWidth() - getInsets().getHorizontal();
            if (_cursx > _txoff + avail) {
                _txoff = _cursx - avail;
            } else if (_glyphs != null &&
                    _glyphs.getSize().width - _txoff < avail) {
                _txoff = Math.max(0, _cursx - avail);
            }
        }
    }

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
    protected Text _glyphs;
    protected KeyMap _keymap = new DefaultKeyMap();

    protected int _prefWidth = -1;
    protected boolean _showCursor;
    protected int _cursp, _selp, _cursx, _selx, _txoff;

    protected Background[] _selectionBackgrounds = new Background[getStateCount()];

    protected Rectangle _srect = new Rectangle();

    protected UndoManager _undomgr = new UndoManager();
    protected int _lastUndoId;
    protected CompoundType _lastCompoundType;
}
