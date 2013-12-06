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

package com.threerings.opengl.gui.text;

import java.util.ArrayList;

import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.samskivert.util.IntTuple;

import static com.threerings.opengl.gui.Log.log;

/**
 * Defines the model that underlies the BUI text components.
 */
public class Document
{
    /** Used to listen for changes to this document. */
    public interface Listener
    {
        /**
         * Called when text is inserted into a document. The text will have
         * already been inserted into the document.
         *
         * @param document the document into which text was inserted.
         * @param offset the offset into the document of the inserted text.
         * @param length the length of the inserted text.
         */
        public void textInserted (Document document, int offset, int length);

        /**
         * Called when text is removed from a document. The text will have
         * already been removed from the document.
         *
         * @param document the document from which text was removed.
         * @param offset the offset into the document of the removed text.
         * @param length the length of the removed text.
         */
        public void textRemoved (Document document, int offset, int length);
    }

    /** Registers a document listener. */
    public void addListener (Listener listener)
    {
        if (_listeners == null) {
            _listeners = new ArrayList<Listener>();
        }
        _listeners.add(listener);
    }

    /** Clears a document listener registration. */
    public void removeListener (Listener listener)
    {
        if (_listeners != null) {
            _listeners.remove(listener);
        }
    }

    /** Adds a listener for undoable edits. */
    public void addUndoableEditListener (UndoableEditListener listener)
    {
        _undosup.addUndoableEditListener(listener);
    }

    /** Removes a listener for undoable edits. */
    public void removeUndoableEditListener (UndoableEditListener listener)
    {
        _undosup.removeUndoableEditListener(listener);
    }

    /**
     * Returns the "value" of the document.
     *
     * By default, this is exactly the same as the text of the document, but subclasses
     * can customize this behavior. For example, a Document that only allows editing
     * of integer values can return an Integer here.
     */
    public Object getValue ()
    {
        return getText();
    }

    /** Returns the entire text of the document. */
    public String getText ()
    {
        return _text;
    }

    /** Returns a subset of the text of the document. */
    public String getText (int offset, int length)
    {
        return getText().substring(offset, offset + length);
    }

    /**
     * Replaces the current contents of the document with the specified text.
     *
     * @param undoId an id used to group compound edits for undo (-1 if not undoable).
     * @return true if the text was changed, false if it was rejected by the
     * document validator.
     */
    public boolean setText (String text, int undoId)
    {
        return (-1 != replace(0, getLength(), text, undoId));
    }

    /**
     * Finds the location of the first word end boundary after the specified position.
     */
    public int indexOfWordEnd (int from)
    {
        // consume the current block of nonword characters, if any, then look for the next
        boolean consumingNonword = true;
        for (int ii = from, nn = _text.length(); ii < nn; ii++) {
            boolean nonword = !Character.isLetterOrDigit(_text.charAt(ii));
            if (consumingNonword) {
                consumingNonword = nonword;
            } else if (nonword) {
                return ii;
            }
        }
        return _text.length();
    }

    /**
     * Finds the location of the last word start boundary before the specified position.
     */
    public int lastIndexOfWordStart (int from)
    {
        // consume the current block of nonword characters, if any, then look for the previous
        boolean consumingNonword = true;
        for (int ii = from - 1; ii >= 0; ii--) {
            boolean nonword = !Character.isLetterOrDigit(_text.charAt(ii));
            if (consumingNonword) {
                consumingNonword = nonword;
            } else if (nonword) {
                return ii + 1;
            }
        }
        return 0;
    }

    /**
     * Given a location in the string, finds the extents (start and end offsets) of connected
     * word or non-word characters (depending on the character at the start).  Used for
     * double-click selection.
     */
    public IntTuple getWordExtents (int start)
    {
        // determine whether the character at the start is a word character
        int length = _text.length();
        boolean word = Character.isLetterOrDigit(_text.charAt(Math.min(start, length - 1)));

        // scan backwards and forwards to find the extents
        while (start > 0 && Character.isLetterOrDigit(_text.charAt(start-1)) == word) {
            start--;
        }
        int end = start;
        while (end < length && Character.isLetterOrDigit(_text.charAt(end)) == word) {
            end++;
        }
        return new IntTuple(start, end);
    }

    /** Returns the number of characters in the document. */
    public int getLength ()
    {
        return _text.length();
    }

    /**
     * Move the cursor, returning the new cursor position.
     */
    public int moveCursor (int position, int offset)
    {
        return Math.max(0, Math.min(position + offset, getLength()));
    }

    /**
     * Inserts the specified text at the specified offset.
     *
     * @param undoId an id used to group compound edits for undo (-1 if not undoable).
     * @return the new cursor position, or -1 if the edit was rejected.
     */
    public int insert (int offset, String text, int undoId)
    {
        return replace(offset, 0, text, undoId);
    }

    /**
     * Deletes specified run of text at the specified offset.
     *
     * @param offset a starting (cursor) position for the edit.
     * @param length the length of the area to remove. Can be negative to "delete leftwards".
     * @param undoId an id used to group compound edits for undo (-1 if not undoable).
     * @return the new cursor position, or -1 if the edit was rejected.
     */
    public int remove (int offset, int length, int undoId)
    {
        return replace(offset, length, "", undoId);
    }

    /**
     * Replaces the specified run of text with the supplied new text.
     *
     * @param offset a starting (cursor) position for the edit.
     * @param length the length of the area to replace with any new text. Can be negative
     *  to "edit leftwards".
     * @param text text to insert at the offset, or "".
     * @param undoId an id used to group compound edits for undo (-1 if not undoable).
     * @return the new cursor position, or -1 if the edit was rejected.
     */
    public int replace (final int offset, final int length, final String text, int undoId)
    {
        int docLength = _text.length();
        int insLength = text.length();
        final int cutStart = Math.max(0, offset + Math.min(length, 0));
        int cutEnd = Math.min(docLength, offset + Math.max(length, 0));

        String ntext = new StringBuilder()
            .append(_text, 0, cutStart)
            .append(text)
            .append(_text, cutEnd, docLength)
            .toString();
        if (!validateEdit(_text, ntext)) {
            return -1;
        }

        if (undoId > 0) {
            if (undoId != _lastUndoId) {
                // rather than using the "real" compound edit, which will only add edits while it's
                // in progress, we create a simpler compound edit that we can post immediately and
                // keep adding events to it until the undo id changes
                _undosup.postEdit(_compoundEdit = new AbstractUndoableEdit() {
                    public boolean addEdit (UndoableEdit edit) {
                        // we use "significance" as the condition for whether we can combine
                        // events.  this is useful for making sure that the compound events
                        // aren't compounded with each other in the UndoManager
                        return edit.isSignificant() ? false : _edits.add(edit);
                    }
                    public void undo () {
                        super.undo();
                        for (int ii = _edits.size() - 1; ii >= 0; ii--) {
                            _edits.get(ii).undo();
                        }
                    }
                    public void redo () {
                        super.redo();
                        for (int ii = 0, nn = _edits.size(); ii < nn; ii++) {
                            _edits.get(ii).redo();
                        }
                    }
                    protected ArrayList<UndoableEdit> _edits = new ArrayList<UndoableEdit>();
                });
            }
            final String otext = _text.substring(cutStart, cutEnd);
            _compoundEdit.addEdit(new AbstractUndoableEdit() {
                public void undo () {
                    super.undo();
                    replace(cutStart, text.length(), otext, -1);
                }
                public void redo () {
                    super.redo();
                    replace(offset, length, text, -1);
                }
                public boolean isSignificant () {
                    return false;
                }
            });
        }
        _lastUndoId = undoId;

        _text = ntext;
        int cutLength = cutEnd - cutStart;
        if (cutLength > 0) {
            notify(false, cutStart, cutLength);
        }
        if (insLength > 0) {
            notify(true, cutStart, insLength);
        }
        return cutStart + insLength;
    }

    /**
     * Provides an opportunity for edits to be rejected before being applied to
     * the document.
     *
     * @return true if the document should be configured with the specified new
     * text, false if the old text should remain.
     */
    protected boolean validateEdit (String oldText, String newText)
    {
        return true;
    }

    /**
     * Notifies document listeners.
     */
    protected void notify (boolean inserted, int offset, int length)
    {
        if (_listeners != null) {
            String action = inserted ? "insertion" : "removal";
            for (int ii = _listeners.size()-1; ii >= 0; ii--) {
                Listener list = _listeners.get(ii);
                try {
                    if (inserted) {
                        list.textInserted(this, offset, length);
                    } else {
                        list.textRemoved(this, offset, length);
                    }
                } catch (Throwable t) {
                    log.warning("Document listener choked on " +
                                action + " [doc=" + this +
                                ", offset=" + offset + ", length=" + length +
                                ", listener=" + list + "].", t);
                }
            }
        }
    }

    protected String _text = "";
    protected ArrayList<Listener> _listeners;
    protected UndoableEditSupport _undosup = new UndoableEditSupport(this);
    protected UndoableEdit _compoundEdit;
    protected int _lastUndoId;
}
