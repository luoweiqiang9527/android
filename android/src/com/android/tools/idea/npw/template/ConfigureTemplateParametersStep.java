/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.tools.idea.npw.template;

import com.android.builder.model.SourceProvider;
import com.android.sdklib.AndroidVersion;
import com.android.tools.idea.model.AndroidModuleInfo;
import com.android.tools.idea.npw.FormFactorUtils;
import com.android.tools.idea.npw.assetstudio.AndroidIconType;
import com.android.tools.idea.npw.assetstudio.NotificationIconStep;
import com.android.tools.idea.npw.project.AndroidPackageUtils;
import com.android.tools.idea.npw.project.AndroidProjectPaths;
import com.android.tools.idea.npw.template.components.*;
import com.android.tools.idea.templates.*;
import com.android.tools.idea.ui.LabelWithEditLink;
import com.android.tools.idea.ui.ProportionalLayout;
import com.android.tools.idea.ui.TooltipLabel;
import com.android.tools.idea.ui.properties.BindingsManager;
import com.android.tools.idea.ui.properties.InvalidationListener;
import com.android.tools.idea.ui.properties.ObservableProperty;
import com.android.tools.idea.ui.properties.ObservableValue;
import com.android.tools.idea.ui.properties.core.*;
import com.android.tools.idea.ui.properties.expressions.Expression;
import com.android.tools.idea.ui.properties.swing.IconProperty;
import com.android.tools.idea.ui.properties.swing.SelectedItemProperty;
import com.android.tools.idea.ui.properties.swing.TextProperty;
import com.android.tools.idea.ui.properties.swing.VisibleProperty;
import com.android.tools.idea.ui.wizard.StudioWizardStepPanel;
import com.android.tools.idea.ui.wizard.Validator;
import com.android.tools.idea.ui.wizard.WizardUtils;
import com.android.tools.idea.wizard.model.ModelWizard;
import com.android.tools.idea.wizard.model.ModelWizardStep;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.ui.*;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.sdk.AndroidPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

import static com.android.tools.idea.templates.KeystoreUtils.getDebugKeystore;
import static com.android.tools.idea.templates.TemplateMetadata.*;

/**
 * A step which takes a {@link Template} (generated by a template.xml file) and wraps a UI around
 * it, allowing a user to modify its various parameters.
 *
 * Far from being generic data, the template edited by this step is very Android specific, and
 * needs to be aware of things like the current project/module, package name, min supported API,
 * previously configured values, etc.
 */
public final class ConfigureTemplateParametersStep extends ModelWizardStep<RenderTemplateModel> {

  private static final String PROJECT_LOCATION_ID = "projectLocation";

  private final List<SourceProvider> mySourceSets;
  private final StringProperty myPackageName = new StringValueProperty();

  private final BindingsManager myBindings = new BindingsManager();
  private final LoadingCache<File, Optional<Icon>> myThumbnailsCache = IconLoader.createLoadingCache();
  private final Map<Parameter, RowEntry> myParameterRows = Maps.newHashMap();
  private final Map<Parameter, Object> myUserValues = Maps.newHashMap();

  private final StringEvaluator myEvaluator = new StringEvaluator();

  private final StringProperty myThumbPath = new StringValueProperty();

  private final StudioWizardStepPanel myStudioPanel;

  /**
   * All parameters are calculated for validity every time any of them changes, and the first error
   * found is set here. This is then registered as its own validator with {@link #myStudioPanel}.
   * This vastly simplifies validation, as we no longer have to worry about implicit relationships
   * between parameters (where changing one, like the package name, makes another valid/invalid).
   */
  private final StringProperty myInvalidParameterMessage = new StringValueProperty();

  private JPanel myRootPanel;
  private JLabel myTemplateThumbLabel;
  private JPanel myParametersPanel;
  private JSeparator myFooterSeparator;
  private TooltipLabel myParameterDescriptionLabel;
  private JBScrollPane myParametersScrollPane;
  private JLabel myTemplateDescriptionLabel;

  private EvaluationState myEvaluationState = EvaluationState.NOT_EVALUATING;

  public ConfigureTemplateParametersStep(@NotNull RenderTemplateModel model,
                                         @NotNull String title,
                                         @NotNull String initialPackageName,
                                         @NotNull List<SourceProvider> sourceSets) {
    super(model, title);

    mySourceSets = sourceSets;
    myPackageName.set(initialPackageName);

    if (mySourceSets.size() > 0) {
      getModel().getSourceSet().setValue(mySourceSets.get(0));
    }

    myStudioPanel = new StudioWizardStepPanel(this, myRootPanel);

    myParameterDescriptionLabel.setScope(myParametersPanel);
    myParametersScrollPane.setBorder(IdeBorderFactory.createEmptyBorder());

    // Add an extra blank line under the template description to separate it from the main body
    myTemplateDescriptionLabel.setBorder(IdeBorderFactory.createEmptyBorder(0, 0, myTemplateDescriptionLabel.getFont().getSize(), 0));
  }

  private static Logger getLog() {
    return Logger.getInstance(ConfigureTemplateParametersStep.class);
  }

  /**
   * Given a parameter, return a String key we can use to interact with IntelliJ's
   * {@link RecentsManager} system.
   */
  @NotNull
  private static String getRecentsKeyForParameter(@NotNull Parameter parameter) {
    return "android.template." + parameter.id;
  }

  /**
   * Helper method for converting two paths relative to one another into a String path, since this
   * ends up being a common pattern when creating values to put into our template's data model.
   */
  @Nullable
  private static String getRelativePath(@NotNull File base, @NotNull File file) {
    String finalPath = FileUtil.getRelativePath(base, file);
    if (finalPath != null) {
      finalPath = FileUtil.toSystemIndependentName(finalPath);
    }
    return finalPath;
  }

  @NotNull
  @Override
  protected Collection<? extends ModelWizardStep> createDependentSteps() {

    if (getModel().getTemplateHandle().getMetadata().getIconType() == AndroidIconType.NOTIFICATION) {
      return Collections.singletonList(new NotificationIconStep(getModel()));
    }
    else {
      return super.createDependentSteps();
    }
  }

  @Override
  protected void onWizardStarting(@NotNull ModelWizard.Facade wizard) {
    final TemplateHandle templateHandle = getModel().getTemplateHandle();

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        // We want to set the label's text AFTER the wizard has been packed. Otherwise, its
        // width calculation gets involved and can really stretch out some wizards if the label is
        // particularly long (see Master/Detail Activity for example).
        myTemplateDescriptionLabel.setText(WizardUtils.toHtmlString(Strings.nullToEmpty(templateHandle.getMetadata().getDescription())));
      }
    }, ModalityState.any());

    final IconProperty thumb = new IconProperty(myTemplateThumbLabel);
    BoolProperty thumbVisibility = new VisibleProperty(myTemplateThumbLabel);
    myBindings.bind(thumb, new Expression<Optional<Icon>>(myThumbPath) {
      @NotNull
      @Override
      public Optional<Icon> get() {
        return myThumbnailsCache.getUnchecked(new File(templateHandle.getRootPath(), myThumbPath.get()));
      }
    });
    myBindings.bind(thumbVisibility, new Expression<Boolean>(thumb) {
      @NotNull
      @Override
      public Boolean get() {
        return thumb.get().isPresent();
      }
    });
    myThumbPath.set(getDefaultThumbnailPath());

    final TextProperty parameterDescription = new TextProperty(myParameterDescriptionLabel);
    myBindings.bind(new VisibleProperty(myFooterSeparator), new Expression<Boolean>(parameterDescription) {
      @NotNull
      @Override
      public Boolean get() {
        return !parameterDescription.get().isEmpty();
      }
    });

    final Collection<Parameter> parameters = templateHandle.getMetadata().getParameters();
    for (final Parameter parameter : parameters) {
      RowEntry row = createRowForParameter(getModel().getModule(), parameter);
      final ObservableValue<?> property = row.getProperty();
      if (property != null) {
        property.addListener(new InvalidationListener() {
          @Override
          public void onInvalidated(@NotNull ObservableValue<?> sender) {
            // If not evaluating, change comes from the user
            if (myEvaluationState == EvaluationState.NOT_EVALUATING) {
              myUserValues.put(parameter, property.get());
              // Evaluate later to prevent modifying Swing values that are locked during read
              enqueueEvaluateParameters();
            }
          }
        });

        final ActionGroup resetParameterGroup = new ActionGroup() {
          @NotNull
          @Override
          public AnAction[] getChildren(@Nullable AnActionEvent e) {
            return new AnAction[]{new ResetParameterAction(parameter)};
          }
        };
        row.getComponent().addMouseListener(new PopupHandler() {
          @Override
          public void invokePopup(Component comp, int x, int y) {
            ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UNKNOWN, resetParameterGroup).getComponent().show(comp, x, y);
          }
        });
      }
      myParameterRows.put(parameter, row);
      row.addToPanel(myParametersPanel);
    }

    if (mySourceSets.size() > 1) {
      RowEntry row = new RowEntry<JComboBox>("Target Source Set", new SourceSetComboProvider(mySourceSets));
      row.setEnabled(mySourceSets.size() > 1);
      row.addToPanel(myParametersPanel);

      //noinspection unchecked
      SelectedItemProperty<SourceProvider> sourceSet = (SelectedItemProperty<SourceProvider>)row.getProperty();
      assert sourceSet != null; // SourceSetComboProvider always sets this
      myBindings.bind(getModel().getSourceSet(), sourceSet);

      sourceSet.addListener(new InvalidationListener() {
        @Override
        public void onInvalidated(@NotNull ObservableValue<?> sender) {
          enqueueEvaluateParameters();
        }
      });
    }

    myStudioPanel.registerValidator(myInvalidParameterMessage, new Validator<String>() {
      @NotNull
      @Override
      public Result validate(@NotNull String message) {
        return (message.isEmpty() ? Result.OK : new Result(Severity.ERROR, message));
      }
    });

    // TODO: This code won't be needed until we migrate this enough to support
    // NewAndroidApplication/template.xml and NewAndroidLibrary/template.xml
    // Add it in then. Probably we can add an optional validator API to ComponentProvider? Then we
    // could move this code into EnumComboProvider and it won't be a hack anymore.
    //
    // if (value instanceof ApiComboBoxItem) {
    //  ApiComboBoxItem selectedItem = (ApiComboBoxItem)value;
    //
    //  if (minApi != null && selectedItem.minApi > minApi.getFeatureLevel()) {
    //    setErrorHtml(String.format("The \"%s\" option for %s requires a minimum API level of %d",
    //                               selectedItem.label, param.name, selectedItem.minApi));
    //    return false;
    //  }
    //  if (buildApi != null && selectedItem.minBuildApi > buildApi) {
    //    setErrorHtml(String.format("The \"%s\" option for %s requires a minimum API level of %d",
    //                               selectedItem.label, param.name, selectedItem.minBuildApi));
    //    return false;
    //  }
    //}
    evaluateParameters();
  }

  /**
   * Every template parameter, based on its type, can generate a row of* components. For example,
   * a text parameter becomes a "Label: Textfield" set, while a list of choices becomes
   * "Label: pulldown".
   * <p/>
   * This method takes an input {@link Parameter} and returns a generated {@link RowEntry} for
   * it, which neatly encapsulates its UI. The caller should use
   * {@link RowEntry#addToPanel(JPanel)} after receiving it.
   */
  private RowEntry<?> createRowForParameter(@Nullable final Module module, @NotNull Parameter parameter) {

    /**
     * Handle custom parameter types first.
     */
    // TODO: Should we extract this logic into an extension point at some point, in order to be
    // more friendly to third-party plugins with templates? Do they need custom UI components?
    if (ATTR_PACKAGE_NAME.equals(parameter.id)) {
      assert parameter.name != null;
      RowEntry<?> rowEntry;
      if (module != null) {
        rowEntry = new RowEntry<EditorComboBox>(parameter.name,
                                                new PackageComboProvider(module.getProject(), parameter, myPackageName.get(),
                                                                         getRecentsKeyForParameter(parameter)));
      }
      else {
        rowEntry = new RowEntry<LabelWithEditLink>(parameter.name, new LabelWithEditLinkProvider(parameter));
      }

      // All ATTR_PACKAGE_NAME providers should be string types and provide StringProperties
      //noinspection unchecked
      StringProperty packageName = (StringProperty)rowEntry.getProperty();
      assert packageName != null;
      myBindings.bindTwoWay(packageName, myPackageName);
      return rowEntry;
    }

    if (ATTR_PARENT_ACTIVITY_CLASS.equals(parameter.id)) {
      if (module != null) {
        assert parameter.name != null;
        return new RowEntry<ReferenceEditorComboWithBrowseButton>(parameter.name, new ActivityComboProvider(module, parameter,
                                                                                                            getRecentsKeyForParameter(
                                                                                                              parameter)));
      }
    }

    /**
     * Handle standard parameter types
     */
    switch (parameter.type) {
      case STRING:
        assert parameter.name != null;
        return new RowEntry<JTextField>(parameter.name, new TextFieldProvider(parameter));
      case BOOLEAN:
        return new RowEntry<JCheckBox>(new CheckboxProvider(parameter), RowEntry.WantGrow.NO);
      case SEPARATOR:
        return new RowEntry<JSeparator>(new SeparatorProvider(parameter), RowEntry.WantGrow.YES);
      case ENUM:
        assert parameter.name != null;
        return new RowEntry<JComboBox>(parameter.name, new EnumComboProvider(parameter));
      default:
        throw new IllegalStateException(
          String.format("Can't create UI for unknown component type: %1$s (%2$s)", parameter.type, parameter.id));
    }
  }

  /**
   * Instead of evaluating all parameters immediately, invoke the request to run later. This
   * option allows us to avoid the situation where a value has just changed, is forcefully
   * re-evaluated immediately, and causes Swing to throw an exception between we're editing a
   * value while it's in a locked read-only state.
   */
  private void enqueueEvaluateParameters() {
    if (myEvaluationState == EvaluationState.REQUEST_ENQUEUED) {
      return;
    }
    myEvaluationState = EvaluationState.REQUEST_ENQUEUED;

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      @Override
      public void run() {
        evaluateParameters();
      }
    }, ModalityState.any());
  }

  /**
   * Run through all parameters for our current template and update their values, including
   * visibility, enabled state, and actual values.
   *
   * Because our templating system is opaque to us, this operation is relatively overkill (we
   * evaluate all parameters every time, not just ones we suspect have changed), but this should
   * only get run in response to user input, which isn't too often.
   */
  private void evaluateParameters() {
    myEvaluationState = EvaluationState.EVALUATING;

    Collection<Parameter> parameters = getModel().getTemplateHandle().getMetadata().getParameters();

    try {
      Map<String, Object> additionalValues = Maps.newHashMap();
      additionalValues.put(ATTR_PACKAGE_NAME, myPackageName.get());
      OptionalProperty<SourceProvider> sourceSet = getModel().getSourceSet();
      if (sourceSet.get().isPresent()) {
        additionalValues.put(ATTR_SOURCE_PROVIDER_NAME, sourceSet.getValue().getName());
      }

      Map<String, Object> allValues = Maps.newHashMap(additionalValues);

      Map<Parameter, Object> parameterValues =
        ParameterValueResolver.resolve(parameters, myUserValues, additionalValues, new ParameterDeduplicator());
      for (Parameter parameter : parameters) {
        Object value = parameterValues.get(parameter);
        if (value == null) continue;
        myParameterRows.get(parameter).setValue(value);
        allValues.put(parameter.id, value);
      }

      for (Parameter parameter : parameters) {
        String enabledStr = Strings.nullToEmpty(parameter.enabled);
        if (!enabledStr.isEmpty()) {
          boolean enabled = myEvaluator.evaluateBooleanExpression(enabledStr, allValues, true);
          myParameterRows.get(parameter).setEnabled(enabled);
        }

        String visibilityStr = Strings.nullToEmpty(parameter.visibility);
        if (!visibilityStr.isEmpty()) {
          boolean visible = myEvaluator.evaluateBooleanExpression(visibilityStr, allValues, true);
          myParameterRows.get(parameter).setVisible(visible);
        }
      }

      // Aggressively update the icon path just in case it changed
      myThumbPath.set(getCurrentThumbnailPath());
    }
    catch (CircularParameterDependencyException e) {
      getLog().error("Circular dependency between parameters in template %1$s", e, getModel().getTemplateHandle().getMetadata().getTitle());
    }
    finally {
      myEvaluationState = EvaluationState.NOT_EVALUATING;
    }

    myInvalidParameterMessage.set(Strings.nullToEmpty(validateAllParameters()));
  }

  /**
   * Get the default thumbnail path, which is useful at initialization time before we have all
   * parameters set up.
   */
  @NotNull
  private String getDefaultThumbnailPath() {
    return Strings.nullToEmpty(getModel().getTemplateHandle().getMetadata().getThumbnailPath());
  }

  /**
   * Get the current thumbnail path, based on current parameter values.
   */
  @NotNull
  private String getCurrentThumbnailPath() {
    return Strings.nullToEmpty(getModel().getTemplateHandle().getMetadata().getThumbnailPath(new Function<String, Object>() {
      @Nullable
      @Override
      public Object apply(String parameterId) {
        Parameter parameter = getModel().getTemplateHandle().getMetadata().getParameter(parameterId);
        ObservableValue<?> property = myParameterRows.get(parameter).getProperty();
        return property != null ? property.get() : null;
      }
    }));
  }

  @Nullable
  private String validateAllParameters() {
    String message = null;

    Collection<Parameter> parameters = getModel().getTemplateHandle().getMetadata().getParameters();
    Module module = getModel().getModule();
    SourceProvider sourceSet = getModel().getSourceSet().getValueOrNull();

    for (Parameter parameter : parameters) {
      ObservableValue<?> property = myParameterRows.get(parameter).getProperty();
      if (property == null) {
        continue;
      }

      Set<Object> relatedValues = getRelatedValues(parameter);
      message = parameter.validate(module.getProject(), module, sourceSet, myPackageName.get(), property.get(), relatedValues);

      if (message != null) {
        break;
      }
    }

    return message;
  }

  @NotNull
  @Override
  protected JComponent getComponent() {
    return myStudioPanel;
  }

  @Nullable
  @Override
  protected JComponent getPreferredFocusComponent() {
    Component[] children = myParametersPanel.getComponents();
    //noinspection ForLoopReplaceableByForEach
    for (int i = 0; i < children.length; i++) {
      JComponent child = (JComponent)children[i];
      boolean isContainer = child.getComponentCount() > 0;
      if (!isContainer && child.isFocusable() && child.isVisible()) {
        return child;
      }
    }

    return null;
  }

  @NotNull
  @Override
  protected ObservableBool canGoForward() {
    return getModel().getSourceSet().isPresent().and(myStudioPanel.hasErrors().not());
  }

  private void createUIComponents() {
    myParametersPanel = new JPanel(ProportionalLayout.fromString("Fit,*", 10));
  }

  @Override
  public void dispose() {
    myBindings.releaseAll();
    myThumbnailsCache.invalidateAll();
  }

  /**
   * When finished with this step, calculate and install a bunch of values that will be used in our
   * template's <a href="http://freemarker.incubator.apache.org/docs/dgui_quickstart_basics.html">data model.</a>
   */
  @Override
  protected void onProceeding() {

    AndroidFacet facet = getModel().getFacet();
    Module module = getModel().getModule();

    // canGoForward guarantees this optional value is present
    SourceProvider sourceSet = getModel().getSourceSet().getValue();

    // This is always non-null when a sourceSet is valid (see also docs for getModel().getPaths())
    AndroidProjectPaths paths = getModel().getPaths();
    assert paths != null;

    File moduleRoot = paths.getModuleRoot();
    if (moduleRoot == null) {
      getLog()
        .error(String.format("%s failure: can't create files because module root is not found. Please report this error.", getTitle()));
      return;
    }

    /**
     * Some parameter values should be saved for later runs through this wizard, so do that first.
     */
    for (RowEntry rowEntry : myParameterRows.values()) {
      rowEntry.accept();
    }

    /**
     * Prepare the template data-model, starting from scratch and filling in all values we know
     */
    Map<String, Object> templateValues = getModel().getTemplateValues();
    templateValues.clear();

    // TODO: Add support for new projects as the wizard migration grows to encompass them
    templateValues.put(ATTR_IS_NEW_PROJECT, false);

    templateValues.put(ATTR_PACKAGE_NAME, myPackageName.get());
    templateValues.put(ATTR_SOURCE_PROVIDER_NAME, sourceSet.getName());

    for (Parameter parameter : myParameterRows.keySet()) {
      ObservableValue<?> property = myParameterRows.get(parameter).getProperty();
      if (property != null) {
        templateValues.put(parameter.id, property.get());
      }
    }

    try {
      templateValues.put(ATTR_DEBUG_KEYSTORE_SHA1, KeystoreUtils.sha1(getDebugKeystore(facet)));
    }
    catch (Exception e) {
      getLog().info("Could not compute SHA1 hash of debug keystore.", e);
      templateValues.put(ATTR_DEBUG_KEYSTORE_SHA1, "");
    }

    AndroidPlatform platform = AndroidPlatform.getInstance(getModel().getModule());
    if (platform != null) {
      templateValues.put(ATTR_BUILD_API, platform.getTarget().getVersion().getFeatureLevel());
      templateValues.put(ATTR_BUILD_API_STRING, getBuildApiString(platform.getTarget().getVersion()));
    }

    /**
     * Register the resource directories associated with the active source provider
     */
    templateValues.put(ATTR_PROJECT_OUT, FileUtil.toSystemIndependentName(moduleRoot.getAbsolutePath()));

    File srcDir = paths.getSrcDirectory();
    File testDir = paths.getTestDirectory();
    if (srcDir != null && testDir != null) {
      String packageAsDir = myPackageName.get().replace('.', File.separatorChar);

      srcDir = new File(srcDir, packageAsDir);
      testDir = new File(testDir, packageAsDir);

      templateValues.put(ATTR_SRC_DIR, getRelativePath(moduleRoot, srcDir));
      templateValues.put(ATTR_SRC_OUT, FileUtil.toSystemIndependentName(srcDir.getAbsolutePath()));

      templateValues.put(ATTR_TEST_DIR, getRelativePath(moduleRoot, testDir));
      templateValues.put(ATTR_TEST_OUT, FileUtil.toSystemIndependentName(testDir.getAbsolutePath()));
    }

    File resDir = paths.getResDirectory();
    if (resDir != null) {
      templateValues.put(ATTR_RES_DIR, getRelativePath(moduleRoot, resDir));
      templateValues.put(ATTR_RES_OUT, FileUtil.toSystemIndependentName(resDir.getPath()));
    }

    File manifestDir = paths.getManifestDirectory();
    if (manifestDir != null) {
      templateValues.put(ATTR_MANIFEST_DIR, getRelativePath(moduleRoot, manifestDir));
      templateValues.put(ATTR_MANIFEST_OUT, FileUtil.toSystemIndependentName(manifestDir.getPath()));
    }

    File aidlDir = paths.getAidlDirectory();
    if (aidlDir != null) {
      templateValues.put(ATTR_AIDL_DIR, getRelativePath(moduleRoot, aidlDir));
      templateValues.put(ATTR_AIDL_OUT, FileUtil.toSystemIndependentName(aidlDir.getPath()));
    }

    /**
     * Register application-wide settings
     */
    String applicationPackage = AndroidPackageUtils.getPackageForApplication(getModel().getFacet());
    if (!myPackageName.get().equals(applicationPackage)) {
      templateValues.put(ATTR_APPLICATION_PACKAGE, AndroidPackageUtils.getPackageForApplication(facet));
    }

    AndroidModuleInfo moduleInfo = AndroidModuleInfo.get(facet);
    AndroidVersion minSdkVersion = moduleInfo.getMinSdkVersion();
    String minSdkName = minSdkVersion.getApiString();

    templateValues.put(ATTR_MIN_API, minSdkName);
    templateValues.put(ATTR_TARGET_API, moduleInfo.getTargetSdkVersion().getApiLevel());
    templateValues.put(ATTR_MIN_API_LEVEL, minSdkVersion.getFeatureLevel());

    templateValues.put(ATTR_IS_LIBRARY_MODULE, facet.isLibraryProject());

    templateValues.put(PROJECT_LOCATION_ID, module.getProject().getBasePath());

    // We're really interested in the directory name on disk, not the module name. These will be different if you give a module the same
    // name as its containing project.
    String moduleName = new File(module.getModuleFilePath()).getParentFile().getName();
    templateValues.put(FormFactorUtils.ATTR_MODULE_NAME, moduleName);
  }

  /**
   * Fetches the values of all parameters that are related to the target parameter. This is useful
   * information when validating a parameter's value.
   */
  private Set<Object> getRelatedValues(@NotNull Parameter parameter) {
    Set<Object> relatedValues = Sets.newHashSet();
    for (Parameter related : parameter.template.getRelatedParams(parameter)) {
      ObservableValue<?> property = myParameterRows.get(related).getProperty();
      if (property == null) continue;

      relatedValues.add(property.get());
    }
    return relatedValues;
  }

  /**
   * Because the FreeMarker templating engine is mostly opaque to us, any time any parameter
   * changes, we need to re-evaluate all parameters. Parameter evaluation can be started
   * immediately via {@link #evaluateParameters()} or with a delay using
   * {@link #enqueueEvaluateParameters()}.
   */
  private enum EvaluationState {
    NOT_EVALUATING,
    REQUEST_ENQUEUED,
    EVALUATING,
  }

  /**
   * A template is broken down into separate fields, each which is given a row with optional
   * header. This class wraps all UI elements in the row, providing methods for managing them.
   */
  private static final class RowEntry<T extends JComponent> {
    @Nullable private final JPanel myHeader;
    @NotNull private final ComponentProvider<T> myComponentProvider;
    @NotNull private final T myComponent;
    @Nullable private ObservableProperty<?> myProperty;
    @NotNull private WantGrow myWantGrow;

    public RowEntry(@NotNull String headerText, @NotNull ComponentProvider<T> componentProvider) {
      myHeader = new JPanel(new FlowLayout(FlowLayout.LEFT));
      myHeader.add(new JBLabel(headerText + ":"));
      myHeader.add(Box.createHorizontalStrut(20));
      myWantGrow = WantGrow.NO;
      myComponentProvider = componentProvider;
      myComponent = componentProvider.createComponent();
      myProperty = componentProvider.createProperty(myComponent);
    }

    public RowEntry(@NotNull ParameterComponentProvider<T> componentProvider, @NotNull WantGrow stretch) {
      myHeader = null;
      myWantGrow = stretch;
      myComponentProvider = componentProvider;
      myComponent = componentProvider.createComponent();
      myProperty = componentProvider.createProperty(myComponent);
    }

    @Nullable
    public ObservableValue<?> getProperty() {
      return myProperty;
    }

    public void addToPanel(@NotNull JPanel panel) {
      assert panel.getLayout().getClass().equals(ProportionalLayout.class);
      int row = panel.getComponentCount();

      if (myHeader != null) {
        panel.add(myHeader, new ProportionalLayout.Constraint(row, 0));
        assert myWantGrow == WantGrow.NO;
      }

      int colspan = myWantGrow == WantGrow.YES ? 2 : 1;
      panel.add(myComponent, new ProportionalLayout.Constraint(row, 1, colspan));
    }

    public void setEnabled(boolean enabled) {
      if (myHeader != null) {
        myHeader.setEnabled(enabled);
      }
      myComponent.setEnabled(enabled);
    }

    public void setVisible(boolean visible) {
      if (myHeader != null) {
        myHeader.setVisible(visible);
      }
      myComponent.setVisible(visible);
    }

    public <V> void setValue(@NotNull V value) {
      assert myProperty != null;
      //noinspection unchecked Should always be true if registration is done correctly
      ((ObservableProperty<V>)myProperty).set(value);
    }

    @NotNull
    public JComponent getComponent() {
      return myComponent;
    }

    public void accept() {
      myComponentProvider.accept(myComponent);
    }

    /**
     * A row is usually broken into two columns, but the item can optionally grow into both columns
     * if it doesn't have a header.
     */
    public enum WantGrow {
      NO,
      YES,
    }
  }

  private final class ParameterDeduplicator implements ParameterValueResolver.Deduplicator {
    @Override
    @Nullable
    public String deduplicate(@NotNull Parameter parameter, @Nullable String value) {
      if (Strings.isNullOrEmpty(value) || !parameter.constraints.contains(Parameter.Constraint.UNIQUE)) {
        return value;
      }

      String suggested = value;
      String extPart = Strings.emptyToNull(Files.getFileExtension(value));
      String namePart = value.replace("." + extPart, "");

      // Remove all trailing digits, because we probably were the ones that put them there.
      // For example, if two parameters affect each other, say "Name" and "Layout", you get this:
      // Step 1) Resolve "Name" -> "Name2", causes related "Layout" to become "Layout2"
      // Step 2) Resolve "Layout2" -> "Layout22"
      // Although we may possibly strip real digits from a name, it's much more likely we're not,
      // and a user can always modify the related value manually in that rare case.
      namePart = namePart.replaceAll("\\d*$", "");
      Joiner filenameJoiner = Joiner.on('.').skipNulls();

      int suffix = 2;
      Module module = getModel().getModule();
      Project project = module.getProject();
      Set<Object> relatedValues = getRelatedValues(parameter);
      SourceProvider sourceSet = getModel().getSourceSet().getValueOrNull();
      while (!parameter.uniquenessSatisfied(project, module, sourceSet, myPackageName.get(), suggested, relatedValues)) {
        suggested = filenameJoiner.join(namePart + suffix, extPart);
        suffix++;
      }
      return suggested;
    }
  }

  /**
   * Right-click context action which lets the user clear any modifications they made to a
   * parameter. Once cleared, the parameter is re-evaluated.
   */
  private final class ResetParameterAction extends AnAction {
    @NotNull private final Parameter myParameter;

    public ResetParameterAction(@NotNull Parameter parameter) {
      super("Restore default value", "Discards any user modifications made to this parameter", AllIcons.General.Reset);
      myParameter = parameter;
    }

    @Override
    public void update(AnActionEvent e) {
      e.getPresentation().setEnabled(myUserValues.containsKey(myParameter));
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      myUserValues.remove(myParameter);
      evaluateParameters();
    }
  }
}
