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
package com.android.tools.idea.uibuilder.handlers.constraint;

import com.android.sdklib.devices.Device;
import com.android.sdklib.devices.State;
import com.android.tools.idea.configurations.Configuration;
import com.android.tools.idea.uibuilder.actions.SetZoomAction;
import com.android.tools.idea.uibuilder.graphics.NlConstants;
import com.android.tools.idea.uibuilder.model.ModelListener;
import com.android.tools.idea.uibuilder.model.NlComponent;
import com.android.tools.idea.uibuilder.model.NlModel;
import com.android.tools.idea.uibuilder.property.NlPropertiesManager;
import com.android.tools.idea.uibuilder.surface.*;
import com.android.tools.sherpa.drawing.BlueprintColorSet;
import com.android.tools.sherpa.drawing.ColorSet;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

import static com.android.tools.idea.uibuilder.surface.DesignSurface.ScreenMode.BOTH;

/**
 * UI component for Navigator Panel showing a miniature representation of the DesignSurface
 * allowing to easily scroll inside the DesignSurface when the UI builder is zoomed.
 * The panel can be collapsed and expanded. The default state is collapsed
 */
public class WidgetNavigatorPanel extends JPanel
  implements DesignSurfaceListener, PanZoomListener, ModelListener, NlPropertiesManager.DesignSurfaceChangedListener {

  private static final String TITLE = "Zoom and pan";
  private static final int SCREEN_SPACE = NlConstants.SCREEN_DELTA;
  private static final Dimension EXPENDED_PREFERRED_SIZE = new Dimension(200, 216);
  private static final Dimension COLLAPSED_PREFERRED_SIZE = new Dimension(200, 20);

  private static final BlueprintColorSet BLUEPRINT_COLOR_SET = new BlueprintColorSet();
  private static final JBColor DRAWING_SURFACE_RECTANGLE_COLOR = new JBColor(Gray._160, Gray._160);
  private static final JBColor OVERLAY_COLOR = new JBColor(new Color(232, 232, 232, 127), new Color(80, 80, 80, 127));
  private static final JBColor NORMAL_SCREEN_VIEW_COLOR = JBColor.WHITE;
  private static final Color BLUEPRINT_SCREEN_VIEW_COLOR = BLUEPRINT_COLOR_SET.getBackground();
  private static final Color COMPONENT_STROKE_COLOR = BLUEPRINT_COLOR_SET.getFrames();

  private static final Color BACKGROUND_COLOR = BLUEPRINT_COLOR_SET.getBackground();
  private final ColorSet myColorSet;
  private final CardLayout myCenterLayout;
  private final JPanel myCenterPanel;
  private ActionButtonPanel myActionButtonPanel;
  private MiniMap myMiniMap;

  @Nullable private DesignSurface myDesignSurface;
  private NlComponent myComponent;
  private Dimension myCurrentScreenViewSize;
  private Dimension myDesignSurfaceSize;
  private Dimension myDeviceSize;
  private double myScreenViewScale;
  private double myDeviceScale;
  private Point myDesignSurfaceOffset;
  private Point mySecondScreenOffset;
  private int myCenterOffset;
  private boolean myIsZoomed;
  private int myYScreenNumber;
  private int myXScreenNumber;
  private int myScaledScreenSpace;

  public WidgetNavigatorPanel(@NotNull NlPropertiesManager propertiesManager) {
    super(new BorderLayout());

    propertiesManager.addSurfaceChangedListener(this);

    myColorSet = BLUEPRINT_COLOR_SET;

    myCenterLayout = new CardLayout();
    myCenterPanel = new JPanel(myCenterLayout);
    myCenterPanel.add(new JLabel(TITLE));
    setPreferredSize(COLLAPSED_PREFERRED_SIZE);

    add(myCenterPanel, BorderLayout.CENTER);
    add(new JSeparator(), BorderLayout.SOUTH);

    final DesignSurface designSurface = propertiesManager.getDesignSurface();
    if (designSurface == null) {
      return;
    }
    setSurface(designSurface);

    // Adding Action Buttons
    myActionButtonPanel = new ActionButtonPanel();
    add(myActionButtonPanel, BorderLayout.EAST);
    myMiniMap = new MiniMap();
    myCenterPanel.add(myMiniMap);

    // Listening to mouse event
    final MouseInteractionListener listener = new MouseInteractionListener();
    addMouseListener(listener);
    addMouseMotionListener(listener);
    addMouseWheelListener(listener);

    myDesignSurfaceOffset = new Point();
    mySecondScreenOffset = new Point();
    setBorder(BorderFactory.createEmptyBorder(2, 2, 4, 2)); // Margins

    configureUI(myComponent);
  }

  /**
   * Panel Holding the Zoom and Collapse Actions
   */
  private class ActionButtonPanel extends JPanel {

    private final JComponent myCollapsedComponent;
    private final JComponent myExpandedComponent;
    private final DefaultActionGroup myExpandedActions;

    public ActionButtonPanel() {
      setOpaque(false);
      ActionManager actionManager = ActionManager.getInstance();

      // Collapse and Zoom Button
      myExpandedActions = new DefaultActionGroup();
      updateZoomAction(myDesignSurface);

      ActionToolbar expandedToolbar = actionManager.createActionToolbar("WidgetNavigationPanel", myExpandedActions, false);
      expandedToolbar.setMinimumButtonSize(JBUI.size(26, 24));
      myExpandedComponent = expandedToolbar.getComponent();
      myExpandedComponent.setOpaque(false);
      add(myExpandedComponent, BorderLayout.EAST);

      // Expand Button
      DefaultActionGroup collapsedAction = new DefaultActionGroup(
        new CollapseAction(CollapseAction.EXPAND)
      );
      ActionToolbar collapsedToolbar = actionManager.createActionToolbar("WidgetNavigationPanel", collapsedAction, false);
      collapsedToolbar.setMinimumButtonSize(JBUI.size(26, 24));
      myCollapsedComponent = collapsedToolbar.getComponent();
      myCollapsedComponent.setOpaque(false);
      myCollapsedComponent.setVisible(false);
      add(myCollapsedComponent, BorderLayout.EAST);
      setCollapsed(true);
    }

    private void updateZoomAction(@Nullable DesignSurface designSurface) {
      myExpandedActions.removeAll();
      myExpandedActions.addAll(new DefaultActionGroup(
        new CollapseAction(CollapseAction.COLLAPSE)));

      if (designSurface != null) {
        myExpandedActions.addAll(new DefaultActionGroup(
          new SetZoomAction(designSurface, ZoomType.IN),
          new SetZoomAction(designSurface, ZoomType.OUT),
          new SetZoomAction(designSurface, ZoomType.FIT)
        ));
      }
    }

    public void setCollapsed(boolean collapsed) {
      myCollapsedComponent.setVisible(collapsed);
      myExpandedComponent.setVisible(!collapsed);
    }
  }

  /**
   * Read the values off of the NLcomponent and set up the UI
   *
   * @param component
   */
  public void configureUI(NlComponent component) {
    myComponent = component;
    if (myDesignSurface == null) {
      return;
    }
    computeScale(myDesignSurface.getCurrentScreenView(), myDesignSurface.getSize(),
                 myDesignSurface.getContentSize(null));
    computeOffsets(myDesignSurface.getCurrentScreenView());
  }

  /**
   * Set the selected component. If no component is selected, it will set the root of the previous selected component as the selected one
   *
   * @param selectedComponents
   */
  public void updateComponents(@Nullable List<NlComponent> selectedComponents) {
    if (selectedComponents != null && !selectedComponents.isEmpty()) {
      myComponent = selectedComponents.get(0);
    }
    else if (myComponent != null) {
      // If not component are selected, displays the full screen
      myComponent = myComponent.getRoot();
    }
    configureUI(myComponent);
    myMiniMap.repaint();
  }

  /* implements DesignSurfaceListener */
  @Override
  public void componentSelectionChanged(@NotNull DesignSurface surface, @NotNull List<NlComponent> selectedComponents) {
    updateComponents(selectedComponents);
  }

  @Override
  public void screenChanged(@NotNull DesignSurface surface, @Nullable ScreenView screenView) {
    setSurface(surface);
    assert myDesignSurface != null;
    computeOffsets(myDesignSurface.getCurrentScreenView());
    myMiniMap.repaint();
  }

  /**
   * The model of the design surface changed
   */
  @Override
  public void modelChanged(@NotNull DesignSurface surface, @Nullable NlModel model) {
    setSurface(surface);
    if (model != null) {
      model.addListener(this);
    }
    computeOffsets(surface.getCurrentScreenView());
    myMiniMap.repaint();
  }

  /* implements ModelListener */

  /**
   * A change occurred inside the model object
   *
   * @param model
   */
  @Override
  public void modelChanged(@NotNull NlModel model) {
    if (myDesignSurface != null) {
      updateDeviceConfiguration(myDesignSurface.getConfiguration());
      updateComponents(model.getComponents());
      updateScreenNumber(myDesignSurface);
      myMiniMap.repaint();
    }
  }

  @Override
  public void modelRendered(@NotNull NlModel model) {
  }

  /* Implements PanZoomListener */
  @Override
  public void zoomChanged(DesignSurface designSurface) {
    setSurface(designSurface);
    myMiniMap.repaint();
  }

  @Override
  public void panningChanged(AdjustmentEvent adjustmentEvent) {
    if (myDesignSurface == null) {
      return;
    }
    final ScreenView currentScreenView = myDesignSurface.getCurrentScreenView();
    if (currentScreenView == null) return;

    myCurrentScreenViewSize = currentScreenView.getSize(myCurrentScreenViewSize);
    myDesignSurfaceSize = myDesignSurface.getSize(myDesignSurfaceSize);
    computeScale(currentScreenView, myDesignSurfaceSize, myCurrentScreenViewSize);
    final Point scrollPosition = myDesignSurface.getScrollPosition();
    myDesignSurfaceOffset.setLocation(
      scrollPosition.getX() * myScreenViewScale,
      scrollPosition.getY() * myScreenViewScale
    );
    repaint();
  }

  /* Implement DesignSurfaceChangedListener */
  @Override
  public void surfaceChanged(@Nullable DesignSurface surface) {
    setSurface(surface);
    if (surface != null) {
      final ScreenView currentScreenView = surface.getCurrentScreenView();
      if (currentScreenView != null) {
        final List<NlComponent> selection = currentScreenView.getSelectionModel().getSelection();
        if (selection.isEmpty()) {
          updateComponents(currentScreenView.getModel().getComponents());
        }
        else {
          updateComponents(selection);
        }

        if (myActionButtonPanel != null) {
          myActionButtonPanel.updateZoomAction(myDesignSurface);
        }
      }
    }
    myMiniMap.repaint();
  }

  /**
   * Set the DesignSurface to display the minimap from
   *
   * @param surface
   */
  public void setSurface(@Nullable DesignSurface surface) {
    if (surface == myDesignSurface) {
      return;
    }

    // Removing all listener for the oldSurface
    if (myDesignSurface != null) {
      myDesignSurface.removeListener(this);
      myDesignSurface.removePanZoomListener(this);
      final ScreenView currentScreenView = myDesignSurface.getCurrentScreenView();
      if (currentScreenView != null) {
        currentScreenView.getModel().removeListener(this);
      }
    }

    myDesignSurface = surface;
    if (myDesignSurface == null) {
      return;
    }
    myDesignSurface.addListener(this);
    myDesignSurface.addPanZoomListener(this);

    final ScreenView currentScreenView = myDesignSurface.getCurrentScreenView();
    if (currentScreenView != null) {
      currentScreenView.getModel().addListener(this);
    }

    final Configuration configuration = myDesignSurface.getConfiguration();
    if (configuration != null) {
      updateDeviceConfiguration(configuration);
    }
    updateScreenNumber(surface);
  }

  /**
   * Update the number of screen displayed in X and Y axis
   *
   * @param surface
   */
  private void updateScreenNumber(@NotNull DesignSurface surface) {
    myXScreenNumber = !surface.isStackVertically() && surface.getScreenMode() == BOTH ? 2 : 1;
    myYScreenNumber = surface.isStackVertically() && surface.getScreenMode() == BOTH ? 2 : 1;
  }

  /**
   * Update the screen size depending on the orientation.
   * Should be called whenever a change in the orientation occurred
   *
   * @param configuration The current configuration used by the model
   */
  private void updateDeviceConfiguration(Configuration configuration) {
    final Device device = configuration.getDevice();
    final State deviceState = configuration.getDeviceState();
    if (device != null && deviceState != null) {
      myDeviceSize = device.getScreenSize(deviceState.getOrientation());
    }
  }

  /**
   * Handle all mouse interaction onto the Minimap
   */
  private class MouseInteractionListener implements MouseListener, MouseMotionListener, MouseWheelListener {

    private final Point myMouseOrigin = new Point(0, 0);
    private final Point mySurfaceOrigin = new Point(0, 0);
    private double myNewXOffset;
    private double myNewYOffset;
    private Dimension myScreenViewSize = new Dimension();
    private ScreenView myCurrentScreenView;
    private boolean myCanDrag;

    @Override
    public void mouseDragged(MouseEvent e) {
      if (myCanDrag) {
        myNewXOffset = mySurfaceOrigin.x + e.getX() - myMouseOrigin.x;
        myNewYOffset = mySurfaceOrigin.y + e.getY() - myMouseOrigin.y;

        // Since the scroll is changed, the panningChanged method will be called by the DesignSurface so no
        // need to manually redraw the DesignSurface miniature
        assert myDesignSurface != null;
        myDesignSurface.setScrollPosition(
          (int)Math.round(myNewXOffset / myScreenViewScale),
          (int)Math.round(myNewYOffset / myScreenViewScale)
        );
      }
    }

    /**
     * Check is the clicked happened in the miniature {@link DesignSurface} representation
     *
     * @param e The {@link MouseEvent}
     * @return True if the event happened in the miniature {@link DesignSurface} representation
     */
    public boolean isInDesignSurfaceRectangle(MouseEvent e) {
      assert myDesignSurface != null;
      return e.getX() > myDesignSurfaceOffset.x + myCenterOffset
             && e.getX() < myDesignSurfaceOffset.x + myCenterOffset + myDesignSurface.getWidth() * myScreenViewScale
             && e.getY() > myDesignSurfaceOffset.y
             && e.getY() < myDesignSurfaceOffset.y + myDesignSurface.getHeight() * myScreenViewScale;
    }

    @Override
    public void mousePressed(MouseEvent e) {
      if (myDesignSurface != null && isInDesignSurfaceRectangle(e)) {
        // Init the value for the drag event
        myCurrentScreenView = myDesignSurface.getCurrentScreenView();
        if (myCurrentScreenView == null) {
          return;
        }
        myScreenViewSize = myCurrentScreenView.getSize(myScreenViewSize);
        myMouseOrigin.setLocation(e.getX(), e.getY());
        mySurfaceOrigin.setLocation(myDesignSurfaceOffset.x, myDesignSurfaceOffset.y);
        myCanDrag = true;
      }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
      myCanDrag = false;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
      if (isInDesignSurfaceRectangle(e) && myIsZoomed) {
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      }
      else {
        setCursor(Cursor.getDefaultCursor());
      }
    }

    @Override
    public void mouseExited(MouseEvent e) {
      setCursor(Cursor.getDefaultCursor());
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
      if (myDesignSurface == null) {
        return;
      }

      final int wheelRotation = e.getWheelRotation();
      if (wheelRotation < 0 && myDesignSurface.getScale() <= 2.) {
        for (int i = 0; i > wheelRotation; i--) {
          myDesignSurface.zoomIn();
        }
      }
      else if (wheelRotation > 0 && myDesignSurface.getScale() > 0.1) {
        for (int i = 0; i < wheelRotation; i++) {
          myDesignSurface.zoomOut();
        }
      }
    }
  }

  /**
   * Action Button to toggle the collapsed state
   */
  private class CollapseAction extends AnAction {

    static final int COLLAPSE = 0;
    static final int EXPAND = 1;

    private final int myAction;

    public CollapseAction(int action) {
      myAction = action;
      if (action == 0) {
        getTemplatePresentation().setIcon(AllIcons.Ide.Notification.Collapse);
        getTemplatePresentation().setHoveredIcon(AllIcons.Ide.Notification.CollapseHover);
      }
      else {
        getTemplatePresentation().setIcon(AllIcons.Ide.Notification.Expand);
        getTemplatePresentation().setHoveredIcon(AllIcons.Ide.Notification.ExpandHover);
      }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      if (myAction == 0) {
        collapse();
      }
      else {
        expand();
      }
    }
  }

  /**
   * Collapse the whole panel and display the panel title
   */
  private void collapse() {
    myMiniMap.setVisible(false);
    setPreferredSize(COLLAPSED_PREFERRED_SIZE);
    myCenterLayout.next(myCenterPanel);
    myActionButtonPanel.setCollapsed(true);
  }

  /**
   * Expand the whole panel
   */
  private void expand() {
    myMiniMap.setVisible(true);
    myActionButtonPanel.setCollapsed(false);
    setPreferredSize(EXPENDED_PREFERRED_SIZE);
    myCenterLayout.next(myCenterPanel);
  }

  /**
   * Set the scale ratio to display the miniature of the {@link ScreenView} and {@link DesignSurface} inside this panel.
   * The scale ratio is computed such as both the {@link ScreenView} and the {@link DesignSurface} are always totally visible
   * whatever the value of the zoom is.
   *
   * @param currentScreenView The active {@link ScreenView}
   * @param designSurfaceSize The real size of the {@link DesignSurface}
   * @param screenViewSize    The total size of all the {@link ScreenView} in the {@link DesignSurface}
   */
  private void computeScale(@Nullable ScreenView currentScreenView,
                            @NotNull Dimension designSurfaceSize,
                            @NotNull Dimension screenViewSize) {

    if (currentScreenView == null) {
      return;
    }

    myIsZoomed = designSurfaceSize.getWidth() < currentScreenView.getX() + screenViewSize.getWidth()
                 || designSurfaceSize.getHeight() < currentScreenView.getY() + screenViewSize.getHeight();

    myScreenViewScale = Math.min(
      myMiniMap.getWidth() / screenViewSize.getWidth(),
      myMiniMap.getHeight() / screenViewSize.getHeight()
    );

    double surfaceScale = Math.min(
      myMiniMap.getWidth() / designSurfaceSize.getWidth(),
      myMiniMap.getHeight() / designSurfaceSize.getHeight()
    );

    myScaledScreenSpace = (int)Math.round(SCREEN_SPACE * surfaceScale);

    myDeviceScale = Math.min(
      myMiniMap.getWidth() / myDeviceSize.getWidth() / (double)myXScreenNumber,
      myMiniMap.getHeight() / myDeviceSize.getHeight() / (double)myYScreenNumber
    );

    computeOffsets(currentScreenView);
  }

  /**
   * Set the Offsets of the Screens to draw and the offset to center all the content
   *
   * @param currentScreenView
   */
  private void computeOffsets(@Nullable ScreenView currentScreenView) {
    if (myDesignSurface != null && currentScreenView != null) {
      myCurrentScreenViewSize = currentScreenView.getSize(myCurrentScreenViewSize);
      if (myDesignSurface.getScreenMode() == BOTH) {
        if (myDesignSurface.isStackVertically()) {
          mySecondScreenOffset.setLocation(0, myDeviceSize.getHeight() * myDeviceScale + myScaledScreenSpace);
        }
        else {
          mySecondScreenOffset.setLocation(myDeviceSize.getWidth() * myDeviceScale + myScaledScreenSpace, 0);
        }
      }
    }

    myCenterOffset = (int)Math.round(
      (myMiniMap.getWidth() - myXScreenNumber * myDeviceSize.getWidth() * myDeviceScale - myScaledScreenSpace) / 2.);
  }

  /**
   * JPanel where the miniature are drown
   */
  private class MiniMap extends JPanel {

    @Override
    public void paintChildren(Graphics g) {

      // Clear the graphics
      Graphics2D gc = (Graphics2D)g;
      gc.setBackground(UIUtil.getWindowColor());
      gc.clearRect(0, 0, getWidth(), getHeight());

      if (myDesignSurface != null) {

        final ScreenView currentScreenView = myDesignSurface.getCurrentScreenView();
        if (currentScreenView != null) {

          myDesignSurfaceSize = myDesignSurface.getSize(myDesignSurfaceSize);
          final Dimension contentSize = myDesignSurface.getContentSize(null);
          final ScreenView blueprintView = myDesignSurface.getBlueprintView();

          computeScale(currentScreenView, myDesignSurfaceSize, contentSize);

          gc.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
          gc.setColor(BACKGROUND_COLOR);

          drawContainers(gc, currentScreenView, blueprintView);
          gc.setColor(COMPONENT_STROKE_COLOR);
          if (myComponent != null) {
            drawAllComponents(gc, myComponent.getRoot(), blueprintView);
          }
          super.paintChildren(g);
        }
      }
    }

    /**
     * Recursively draw all components  by finding the root of component then find all its children and grandchildren (BFS)
     *
     * @param gc            the {@link Graphics2D} to draw on
     * @param component     a component in the {@link ScreenView}
     * @param blueprintView the blueprint {@link ScreenView} if it is available
     */
    private void drawAllComponents(Graphics2D gc,
                                   NlComponent component,
                                   @Nullable ScreenView blueprintView) {

      // Save the current color to highlight the selected component then reset the color
      Color color = gc.getColor();
      if (component.getId() != null && component.getId().equals(myComponent.getId())) {
        gc.setColor(myColorSet.getSelectedFrames());

      }
      drawComponent(gc, component, blueprintView);
      gc.setColor(color);

      // Recursively draw the others components
      int childCount = component.getChildCount();
      for (int i = 0; i < childCount; i++) {
        drawAllComponents(gc, component.getChild(i), blueprintView);
      }
    }

    /**
     * Draw on component on all the miniatures {@link ScreenView} available
     *
     * @param gc            the {@link Graphics2D} to draw on
     * @param component     a component in the {@link ScreenView}
     * @param blueprintView the blueprint {@link ScreenView} if it is available
     */
    private void drawComponent(Graphics2D gc,
                               NlComponent component,
                               @Nullable ScreenView blueprintView) {

      final double componentRatio = myDeviceScale;
      gc.drawRect(
        (int)Math.round(myCenterOffset + component.x * componentRatio),
        (int)Math.round(component.y * componentRatio),
        (int)Math.round(component.w * componentRatio),
        (int)Math.round(component.h * componentRatio));

      assert myDesignSurface != null;
      if (myDesignSurface.getScreenMode() == BOTH && blueprintView != null) {
        gc.drawRect(
          (int)Math.round(myCenterOffset + mySecondScreenOffset.x + component.x * componentRatio),
          (int)Math.round(mySecondScreenOffset.y + component.y * componentRatio),
          (int)Math.round(component.w * componentRatio),
          (int)Math.round(component.h * componentRatio)
        );
      }
    }

    /**
     * @param gc                the {@link Graphics2D} to draw on
     * @param currentScreenView the current {@link ScreenView}
     * @param blueprintView     the blueprint {@link ScreenView} if it is availableView
     */
    private void drawContainers(Graphics2D gc,
                                ScreenView currentScreenView,
                                @Nullable ScreenView blueprintView) {

      // Draw the first screen view
      assert myDesignSurface != null;
      if (myDesignSurface.getScreenMode() == DesignSurface.ScreenMode.BLUEPRINT_ONLY) {
        gc.setColor(BLUEPRINT_SCREEN_VIEW_COLOR);
      }
      else {
        gc.setColor(NORMAL_SCREEN_VIEW_COLOR);
      }
      gc.fillRect(
        myCenterOffset,
        0,
        (int)Math.round(myDeviceSize.getWidth() * myDeviceScale),
        (int)Math.round(myDeviceSize.getHeight() * myDeviceScale)
      );

      // Draw the second screenView
      if (myDesignSurface.getScreenMode() == BOTH
          && blueprintView != null) {

        gc.setColor(BLUEPRINT_SCREEN_VIEW_COLOR);
        gc.fillRect(
          myCenterOffset + mySecondScreenOffset.x,
          mySecondScreenOffset.y,
          (int)Math.round(myDeviceSize.getWidth() * myDeviceScale),
          (int)Math.round(myDeviceSize.getHeight() * myDeviceScale)
        );
      }

      if (myIsZoomed) {

        // Rectangle of the drawing surface
        gc.setColor(DRAWING_SURFACE_RECTANGLE_COLOR);

        final Rectangle designSurfaceRect = new Rectangle(
          myCenterOffset + myDesignSurfaceOffset.x,
          myDesignSurfaceOffset.y,
          (int)Math.round((myDesignSurfaceSize.getWidth() - currentScreenView.getX() / 2.) * myScreenViewScale),
          (int)Math.round(myDesignSurfaceSize.getHeight() * myScreenViewScale));

        gc.draw(designSurfaceRect);

        // Darken the non visible parts
        gc.setColor(OVERLAY_COLOR);

        //left
        gc.fillRect(0, 0, designSurfaceRect.x, getHeight());

        //Top
        gc.fillRect(designSurfaceRect.x,
                    0,
                    designSurfaceRect.width,
                    designSurfaceRect.y
        );

        // Right
        gc.fillRect(designSurfaceRect.x + designSurfaceRect.width,
                    0,
                    myMiniMap.getWidth() - designSurfaceRect.x - designSurfaceRect.width + myCenterOffset + myScaledScreenSpace,
                    getHeight());

        // Bottom
        gc.fillRect(designSurfaceRect.x,
                    designSurfaceRect.y + designSurfaceRect.height,
                    designSurfaceRect.width,
                    (int)Math.round(myDeviceSize.getHeight() * myDeviceScale) * myYScreenNumber -
                    designSurfaceRect.y -
                    designSurfaceRect.height
        );
      }
    }
  }
}
