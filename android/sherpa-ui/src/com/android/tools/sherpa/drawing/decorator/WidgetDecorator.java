/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.tools.sherpa.drawing.decorator;

import com.android.tools.sherpa.drawing.ColorSet;
import com.android.tools.sherpa.drawing.ViewTransform;
import com.android.tools.sherpa.structure.Selection;
import com.android.tools.sherpa.drawing.WidgetDraw;
import com.google.tnt.solver.widgets.ConstraintAnchor;
import com.google.tnt.solver.widgets.ConstraintWidget;
import com.google.tnt.solver.widgets.ConstraintWidgetContainer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

/**
 * Base class for painting a widget in blueprint mode
 */
public class WidgetDecorator {

    public static final int BLUEPRINT_STYLE = 0;
    public static final int ANDROID_STYLE = 1;

    private static boolean sShowAllConstraints = false;
    private static boolean sShowTextUI = false;
    private boolean mIsVisible = true;
    private boolean mIsSelected = false;
    private boolean mShowResizeHandles = false;
    private boolean mShowSizeIndicator = false;
    private boolean mShowPercentIndicator = false;
    protected ColorSet mColorSet;

    private EnumSet<WidgetDraw.ANCHORS_DISPLAY> mDisplayAnchorsPolicy =
            EnumSet.of(WidgetDraw.ANCHORS_DISPLAY.NONE);

    private ColorTheme mBackgroundColor;
    private ColorTheme mFrameColor;
    protected ColorTheme mTextColor;
    private ColorTheme mConstraintsColor;

    private ColorTheme.Look mLook;

    protected final ConstraintWidget mWidget;
    private int mStyle;

    /**
     * Utility function to load an image from the resources
     *
     * @param path path of the image
     * @return the image loaded, or null if we couldn't
     */
    public static BufferedImage loadImage(String path) {
        if (path == null) {
            return null;
        }
        try {
            InputStream stream = WidgetDecorator.class.getResourceAsStream(path);
            if (stream != null) {
                BufferedImage image = ImageIO.read(stream);
                return image;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Base constructor
     *
     * @param widget the widget we are decorating
     */
    public WidgetDecorator(ConstraintWidget widget) {
        mWidget = widget;
    }

    public void setColorSet(ColorSet colorSet) {
        if (mColorSet == colorSet) {
            return;
        }
        mColorSet = colorSet;
        if (mColorSet == null) {
            return;
        }
        // Setup the colors we use

        // ColorTheme:
        // subdued
        // normal
        // highlighted
        // selected
        mBackgroundColor = new ColorTheme(
                mColorSet.getSubduedBackground(),
                mColorSet.getBackground(),
                mColorSet.getHighlightedBackground(),
                mColorSet.getSelectedBackground());

        mFrameColor = new ColorTheme(
                mColorSet.getSubduedFrames(),
                mColorSet.getFrames(),
                mColorSet.getHighlightedFrames(),
                mColorSet.getSelectedFrames());

        mTextColor = new ColorTheme(
                mColorSet.getSubduedText(),
                mColorSet.getText(),
                mColorSet.getText(),
                mColorSet.getSelectedText());

        mConstraintsColor = new ColorTheme(
                mColorSet.getSubduedConstraints(),
                mColorSet.getConstraints(),
                mColorSet.getConstraints(),
                mColorSet.getSelectedConstraints());
    }

    /**
     * Setter for the visibility of the widget
     *
     * @param isVisible if true, display the widget
     */
    public void setIsVisible(boolean isVisible) {
        mIsVisible = isVisible;
    }

    /**
     * Getter for the visibility of the widget
     *
     * @return the current visibility status, true if visible
     */
    public boolean isVisible() {
        return mIsVisible;
    }

    /**
     * Returns true if the widget is animating (i.e. another repaint will be needed)
     *
     * @return true if animating
     */
    public boolean isAnimating() {
        if (mColorSet == null) {
            return false;
        }
        if (mBackgroundColor.isAnimating()) {
            return true;
        }
        if (mFrameColor.isAnimating()) {
            return true;
        }
        if (mTextColor.isAnimating()) {
            return true;
        }
        if (mConstraintsColor.isAnimating()) {
            return true;
        }
        return false;
    }

    /**
     * Set showing all constraints for all widgets
     *
     * @param value
     */
    public static void setShowAllConstraints(boolean value) {
        sShowAllConstraints = value;
    }

    /**
     * Set show text UI
     *
     * @param value
     */
    public static void setShowFakeUI(boolean value) {
        sShowTextUI = value;
    }

    /**
     * Accessor returning true if we should show all constraints for all widgets
     *
     * @return true if show all constraints
     */
    public static boolean isShowAllConstraints() {
        return sShowAllConstraints;
    }

    /**
     * Accessor returning true if we want to show the text ui for the widgets
     *
     * @return true if show text ui
     */
    public static boolean isShowFakeUI() {
        return sShowTextUI;
    }

    /**
     * Set the isSelected flag for this decorator
     *
     * @param value
     */
    public void setIsSelected(boolean value) {
        mIsSelected = value;
        if (!mIsSelected) {
            // we reset the percent indicator so that it won't show
            // automatically upon selection (but rather, only if the inspector tells us)
            mShowPercentIndicator = false;
        }
        if (mIsSelected) {
            setLook(ColorTheme.Look.SELECTED);
        } else {
            setLook(ColorTheme.Look.NORMAL);
        }
    }

    /**
     * Accessor for the isSelected flag
     *
     * @return true if the decorator/widget is currently selected, false otherwise
     */
    public boolean isSelected() {
        return mIsSelected;
    }

    /**
     * Set to true to show the resize handle for this decorator widget
     *
     * @param value
     */
    public void setShowResizeHandles(boolean value) {
        mShowResizeHandles = value;
    }

    /**
     * Set to true to show the size indicator for this decorator widget
     *
     * @param value
     */
    public void setShowSizeIndicator(boolean value) {
        mShowSizeIndicator = value;
    }

    /**
     * Set to true to show the percent indicator if any
     *
     * @param value
     */
    public void setShowPercentIndicator(boolean value) {
        mShowPercentIndicator = value;
    }

    /**
     * Set the current look for this decorator
     *
     * @param look the look to use (normal, subdued, selected, etc.)
     */
    public void setLook(ColorTheme.Look look) {
        mLook = look;
    }

    /**
     * Accessor returning the current look
     *
     * @return the current look for this decorator
     */
    public ColorTheme.Look getLook() {
        return mLook;
    }

    /**
     * Apply the current look if needed
     */
    public void applyLook() {
        if (mColorSet == null) {
            return;
        }
        if (mBackgroundColor.getLook() != mLook) {
            mBackgroundColor.setLook(mLook);
            mFrameColor.setLook(mLook);
            mTextColor.setLook(mLook);
            mConstraintsColor.setLook(mLook);
        }
    }

    /**
     * Return the current background color
     *
     * @return current background color
     */
    public Color getBackgroundColor() {
        return mBackgroundColor.getColor();
    }

    /**
     * Main painting function
     *
     * @param transform the view transform
     * @param g         the graphics context
     * @return true if we need to be called again (i.e. if we are animating)
     */
    public boolean onPaint(ViewTransform transform, Graphics2D g) {
        if (mColorSet == null) {
            return false;
        }
        onPaintBackground(transform, g);
        if (mIsSelected) {
            updateShowAnchorsPolicy();
        } else {
            mShowResizeHandles = false;
            mShowSizeIndicator = false;
        }

        if (mColorSet.drawWidgetInfos()) {
            if (mWidget.getVisibility() == ConstraintWidget.INVISIBLE) {
                Color c = mTextColor.getColor();
                g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));
            } else {
                g.setColor(mTextColor.getColor());
            }
            WidgetDraw.drawWidgetInfo(transform, g, mWidget);
        }

        g.setColor(mFrameColor.getColor());
        WidgetDraw.drawWidgetFrame(transform, g, mWidget,
                mColorSet, mDisplayAnchorsPolicy, mShowResizeHandles,
                mShowSizeIndicator, mIsSelected, mStyle);

        return isAnimating();
    }

    /**
     * Paint the background of the widget
     *
     * @param transform the view transform
     * @param g         the graphics context
     */
    public void onPaintBackground(ViewTransform transform, Graphics2D g) {
        if (mColorSet == null) {
            return;
        }
        if (!mColorSet.drawBackground()) {
            return;
        }
        if (!(mWidget instanceof ConstraintWidgetContainer)
                && mWidget.getVisibility() == ConstraintWidget.VISIBLE) {
            int l = transform.getSwingX(mWidget.getDrawX());
            int t = transform.getSwingY(mWidget.getDrawY());
            int w = transform.getSwingDimension(mWidget.getDrawWidth());
            int h = transform.getSwingDimension(mWidget.getDrawHeight());
            g.setColor(mBackgroundColor.getColor());
            if (mBackgroundColor.getLook() != ColorTheme.Look.NORMAL) {
                g.fillRect(l, t, w, h);
            }

            Color bg = new Color(0, 0, 0, 0);
            Color fg = ColorTheme.updateBrightness(mBackgroundColor.getColor(), 1.2f);
            Graphics2D gfill = (Graphics2D) g.create();
            gfill.setPaint(new LinearGradientPaint(l, t, (l + 2), (t + 2),
                    new float[] { 0, .1f, .1001f }, new Color[] { fg, fg, bg },
                    MultipleGradientPaint.CycleMethod.REFLECT)
            );
            gfill.fillRect(l, t, w, h);
            gfill.dispose();
        }
    }

    /**
     * Paint the constraints of this widget
     *
     * @param transform the view transform
     * @param g         the graphics context
     */
    public void onPaintConstraints(ViewTransform transform, Graphics2D g) {
        if (mColorSet == null) {
            return;
        }
        if (mWidget.getVisibility() == ConstraintWidget.GONE) {
            return;
        }
        g.setColor(mConstraintsColor.getColor());
        if (mIsSelected || isShowAllConstraints()) {
            WidgetDraw.drawConstraints(transform, g, mColorSet, mWidget, mIsSelected,
                    mShowPercentIndicator, false);
        }
    }

    /**
     * Update the show anchors policy. Used for selected widgets.
     */
    private void updateShowAnchorsPolicy() {
        mDisplayAnchorsPolicy.clear();
        mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.ALL);
        mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.SELECTED);
        if (mWidget.getParent() instanceof ConstraintWidgetContainer) {
            ConstraintWidgetContainer container =
                    (ConstraintWidgetContainer) mWidget.getParent();
            if (container.handlesInternalConstraints()) {
                mDisplayAnchorsPolicy.clear();
                mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.NONE);
            }
        }
        if (!mShowResizeHandles) {
            mDisplayAnchorsPolicy.clear();
            mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.CONNECTED);
        }
    }

    /**
     * Update the show anchors policy. Used for unselected widgets.
     *
     * @param selectedWidget the current selected widget (if any)
     * @param selectedAnchor the current selected anchor (if any)
     */
    public void updateShowAnchorsPolicy(ConstraintWidget selectedWidget,
            ConstraintAnchor selectedAnchor) {
        mDisplayAnchorsPolicy.clear();
        mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.NONE);
        if (isShowAllConstraints()) {
            if (mWidget.getParent() != null) {
                // we should only show the constraints anchors if our parent doesn't handle
                // the constraints already
                ConstraintWidgetContainer container =
                        (ConstraintWidgetContainer) mWidget.getParent();
                if (!container.handlesInternalConstraints()) {
                    mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.CONNECTED);
                }
            } else {
                mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.CONNECTED);
            }
        }
        if (selectedWidget != null) {
            if (!isShowAllConstraints()) {
                mDisplayAnchorsPolicy.clear();
            }
            ConstraintAnchor left =
                    isConnectedAnchor(selectedWidget, ConstraintAnchor.Type.LEFT, mWidget);
            ConstraintAnchor right =
                    isConnectedAnchor(selectedWidget, ConstraintAnchor.Type.RIGHT, mWidget);
            ConstraintAnchor top =
                    isConnectedAnchor(selectedWidget, ConstraintAnchor.Type.TOP, mWidget);
            ConstraintAnchor bottom =
                    isConnectedAnchor(selectedWidget, ConstraintAnchor.Type.BOTTOM, mWidget);
            ConstraintAnchor baseline =
                    isConnectedAnchor(selectedWidget, ConstraintAnchor.Type.BASELINE, mWidget);
            updateDisplayAnchorSet(mDisplayAnchorsPolicy, left);
            updateDisplayAnchorSet(mDisplayAnchorsPolicy, top);
            updateDisplayAnchorSet(mDisplayAnchorsPolicy, right);
            updateDisplayAnchorSet(mDisplayAnchorsPolicy, bottom);
            updateDisplayAnchorSet(mDisplayAnchorsPolicy, baseline);
        }
        if (selectedAnchor != null) {
            if (selectedAnchor.isConnectionAllowed(mWidget)) {
                if (selectedAnchor.getType() == ConstraintAnchor.Type.BASELINE) {
                    mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.BASELINE);
                } else if (selectedAnchor.getType() == ConstraintAnchor.Type.CENTER) {
                    mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.VERTICAL);
                    mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.HORIZONTAL);
                    if (mWidget == selectedAnchor.getOwner().getParent()) {
                        // only display the center anchor for the parent of the selected widget
                        mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.CENTER);
                    }
                } else if (selectedAnchor.isVerticalAnchor()) {
                    mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.VERTICAL);
                } else {
                    mDisplayAnchorsPolicy.add(WidgetDraw.ANCHORS_DISPLAY.HORIZONTAL);
                }
            }
        }
    }

    /**
     * Check if a given anchor from the selected widget is connected to widget
     *
     * @param selectedWidget the widget we are looking at
     * @param type           the type of constraint anchor we are checking
     * @param widget         the widget we want to know if we connect to
     * @return true if the selectedWidget connects to widget via the given anchor type
     */
    private ConstraintAnchor isConnectedAnchor(ConstraintWidget selectedWidget,
            ConstraintAnchor.Type type,
            ConstraintWidget widget) {
        ConstraintAnchor anchor = selectedWidget.getAnchor(type);
        if (anchor != null && anchor.isConnected() && anchor.getTarget().getOwner() == widget) {
            return anchor.getTarget();
        }
        return null;
    }

    /**
     * Set the given anchor display depending on the type of anchor
     *
     * @param set    the EnumSet encoding which anchors to display
     * @param anchor the anchor we connect to that we thus want to display...
     */
    private void updateDisplayAnchorSet(EnumSet<WidgetDraw.ANCHORS_DISPLAY> set,
            ConstraintAnchor anchor) {
        if (anchor == null) {
            return;
        }
        ConstraintAnchor.Type type = anchor.getType();
        switch (type) {
            case LEFT: {
                set.add(WidgetDraw.ANCHORS_DISPLAY.LEFT);
            }
            break;
            case TOP: {
                set.add(WidgetDraw.ANCHORS_DISPLAY.TOP);
            }
            break;
            case RIGHT: {
                set.add(WidgetDraw.ANCHORS_DISPLAY.RIGHT);
            }
            break;
            case BOTTOM: {
                set.add(WidgetDraw.ANCHORS_DISPLAY.BOTTOM);
            }
            break;
            case BASELINE: {
                set.add(WidgetDraw.ANCHORS_DISPLAY.BASELINE);
            }
            break;
        }
    }

    /**
     * Can be overriden by subclasses to apply the dimension behaviour of the widget
     * (i.e., wrap_content, fix, any..)
     */
    public void applyDimensionBehaviour() {
    }

    /**
     * Can be overriden by subclasses to handle mouse press events
     *
     * @param x         mouse x coordinate
     * @param y         mouse y coordinate
     * @param transform view transform
     * @param selection the current selection of widgets
     */
    public ConstraintWidget mousePressed(float x, float y, ViewTransform transform,
            Selection selection) {
        return null;
    }

    /**
     * Can be overriden by subclasses to handle mouse release events
     *
     * @param x         mouse x coordinate
     * @param y         mouse y coordinate
     * @param transform view transform
     * @param selection the current selection of widgets
     */
    public void mouseRelease(int x, int y, ViewTransform transform, Selection selection) {
    }

    public int getStyle() {
        return mStyle;
    }

    public void setStyle(int style) {
        mStyle = style;
    }
}
