/*
 * Copyright (c) 2010, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

package koma.gui.element.control.skin

import javafx.scene.control.*

/**
 * Parent class to control skins whose contents are virtualized and scrollable.
 * This class handles the interaction with the VirtualFlow class, which is the
 * main class handling the virtualization of the contents of this container.
 */
abstract class KVirtualContainerBase<C, I, T>(
        control: C
): SkinBase<C>(control)
        where C: Control,
              I: ListCell<T> {
    private var itemCountDirty: Boolean = false

    /**
     * The virtualized container which handles the layout and scrolling of
     * all the cells.
     */
    protected abstract val flow: KVirtualFlow<I, T>

    /**
     * the total number of items in this container
     * including those that are currently hidden because they are out of view?
     */
    protected abstract val itemCount: Int

    internal val virtualFlow: KVirtualFlow<I, T>
        get() = flow

    init {
        control.addEventHandler(ScrollToEvent.scrollToTopIndex()) { event ->
            // Fix for RT-24630: The row count in VirtualFlow was incorrect
            // (normally zero), so the scrollTo call was misbehaving.
            if (itemCountDirty) {
                // update row count before we do a scroll
                updateItemCount()
                itemCountDirty = false
            }
            flow.scrollToTop(event.scrollTarget)
        }
    }

    /**
     * This method is called when it is possible that the item count has changed (i.e. scrolling has occurred,
     * the control has resized, etc). This method should recalculate the item count and store that for future
     * use by the [.getItemCount] method.
     */
    protected abstract fun updateItemCount()

    /**
     * Call this method to indicate that the item count should be updated on the next pulse.
     */
    protected fun markItemCountDirty() {
        itemCountDirty = true
    }

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        checkState()
    }

    internal fun getMaxCellWidth(rowsToCount: Int): Double {
        return snappedLeftInset() + flow.getMaxCellWidth(rowsToCount) + snappedRightInset()
    }

    internal fun getVirtualFlowPreferredHeight(rows: Int): Double {
        var height = 1.0

        var i = 0
        while (i < rows && i < itemCount) {
            height += flow.getCellLength(i)
            i++
        }

        return height + snappedTopInset() + snappedBottomInset()
    }

    internal fun checkState() {
        if (itemCountDirty) {
            updateItemCount()
            itemCountDirty = false
        }
    }

    internal fun requestRebuildCells() {
        flow.rebuildCells()
    }

}
