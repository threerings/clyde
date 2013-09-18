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

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;

import com.threerings.opengl.gui.event.InputEvent;
import com.threerings.opengl.gui.icon.Icon;

/**
 * Handles drag and drop, cut and paste operations.
 */
public class TransferHandler
{
    /** Indicates that no source actions are supported. */
    public static final int NONE = 0;

    /** Indicates that only the copy action is supported. */
    public static final int COPY = (1 << 0);

    /** Indicates that the move action is supported. */
    public static final int MOVE = (1 << 1);

    /** Indicates that the copy and move actions are supported. */
    public static final int COPY_OR_MOVE = (COPY | MOVE);

    /**
     * Returns the source actions supported by the specified component.
     *
     * @return a bitwise OR of the action flags, COPY or MOVE.
     */
    public int getSourceActions (Component comp)
    {
        return NONE;
    }

    /**
     * Initiates a drag operation.
     */
    public void exportAsDrag (Component comp, InputEvent event, int action)
    {
        comp.getWindow().getRoot().startDrag(this, comp, action);
    }

    /**
     * Exports from the specified component to the given clipboard.
     */
    public void exportToClipboard (Component comp, Clipboard clipboard, int action)
    {
        Transferable data = createTransferable(comp);
        clipboard.setContents(data, null);
        exportDone(comp, data, action);
    }

    /**
     * Returns the visual representation for the specified transferable, or null to use the
     * default.
     */
    public Icon getVisualRepresentation (Transferable data)
    {
        return null;
    }

    /**
     * Determines whether the specified component can accept an import prior to attempting one.
     */
    public boolean canImport (Component comp, DataFlavor[] transferFlavors)
    {
        return false;
    }

    /**
     * Attempts to import data from the specified component.
     *
     * @return true if the data was successfully imported, false if not.
     */
    public boolean importData (Component comp, Transferable data)
    {
        return false;
    }

    /**
     * Creates the transferable to use as the source for a data transfer.
     */
    protected Transferable createTransferable (Component comp)
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Invoked after data has been exported.
     */
    protected void exportDone (Component source, Transferable data, int action)
    {
        // nothing by default
    }
}
