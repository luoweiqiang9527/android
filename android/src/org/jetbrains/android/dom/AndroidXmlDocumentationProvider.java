package org.jetbrains.android.dom;

import com.android.SdkConstants;
import com.android.ide.common.resources.ResourceUrl;
import com.android.resources.ResourceType;
import com.android.tools.idea.javadoc.AndroidJavaDocRenderer;
import com.android.utils.Pair;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlToken;
import com.intellij.util.xml.*;
import com.intellij.util.xml.reflect.DomAttributeChildDescription;
import com.intellij.util.xml.reflect.DomExtension;
import org.jetbrains.android.dom.attrs.AttributeDefinition;
import org.jetbrains.android.dom.attrs.AttributeDefinitions;
import org.jetbrains.android.dom.attrs.AttributeFormat;
import org.jetbrains.android.dom.converters.AttributeValueDocumentationProvider;
import org.jetbrains.android.dom.wrappers.LazyValueResourceElementWrapper;
import org.jetbrains.android.facet.AndroidFacet;
import org.jetbrains.android.resourceManagers.ResourceManager;
import org.jetbrains.android.resourceManagers.SystemResourceManager;
import org.jetbrains.android.resourceManagers.ValueResourceInfo;
import org.jetbrains.android.util.AndroidUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.android.SdkConstants.*;
import static com.intellij.psi.xml.XmlTokenType.*;

/**
 * @author Eugene.Kudelevsky
 */
public class AndroidXmlDocumentationProvider implements DocumentationProvider {
  @Override
  public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
    if (element instanceof LazyValueResourceElementWrapper) {
      final ValueResourceInfo info = ((LazyValueResourceElementWrapper)element).getResourceInfo();
      return "value resource '" + info.getName() + "' [" + info.getContainingFile().getName() + "]";
    }
    return null;
  }

  @Override
  public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
    return null;
  }

  @Override
  public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (element instanceof LazyValueResourceElementWrapper) {
      LazyValueResourceElementWrapper wrapper = (LazyValueResourceElementWrapper)element;
      ValueResourceInfo resourceInfo = wrapper.getResourceInfo();
      ResourceType type = resourceInfo.getType();
      String name = resourceInfo.getName();

      Module module = ModuleUtilCore.findModuleForPsiElement(element);
      if (module == null) {
        return null;
      }
      AndroidFacet facet = AndroidFacet.getInstance(element);
      if (facet == null) {
        return null;
      }

      ResourceUrl url;
      ResourceUrl originalUrl = originalElement != null ? ResourceUrl.parse(originalElement.getText()) : null;
      if (originalUrl != null && name.equals(originalUrl.name)) {
        url  = originalUrl;
      } else {
        boolean isFramework = false;
        if (originalUrl != null) {
          isFramework = originalUrl.framework;
        } else {
          // Figure out if this resource is a framework file.
          // We really should store that info in the ValueResourceInfo instances themselves.
          // For now, attempt to figure it out
          SystemResourceManager systemResourceManager = facet.getSystemResourceManager();
          VirtualFile containingFile = resourceInfo.getContainingFile();
          if (systemResourceManager != null) {
            VirtualFile parent = containingFile.getParent();
            if (parent != null) {
              VirtualFile resDir = parent.getParent();
              if (resDir != null) {
                isFramework = systemResourceManager.isResourceDir(resDir);
              }
            }
          }
        }

        url = ResourceUrl.create(type, name, isFramework, false);
      }
      return generateDoc(element, url);
    } else if (element instanceof MyResourceElement) {
      return getResourceDocumentation(element, ((MyResourceElement)element).myResource);
    } else if (element instanceof XmlAttributeValue) {
      return getResourceDocumentation(element, ((XmlAttributeValue)element).getValue());
    }
    if (originalElement instanceof XmlToken) {
      XmlToken token = (XmlToken)originalElement;
      if (token.getTokenType() == XML_ATTRIBUTE_VALUE_START_DELIMITER) {
        PsiElement next = token.getNextSibling();
        if (next instanceof XmlToken) {
          token = (XmlToken)next;
        }
      }
      if (token.getTokenType() == XML_ATTRIBUTE_VALUE_TOKEN) {
        return getResourceDocumentation(originalElement, token.getText());
      } else if (token.getTokenType() == XML_DATA_CHARACTERS) {
        String text = token.getText().trim();
        return getResourceDocumentation(originalElement, text);
      }
    }

    if (element instanceof PomTargetPsiElement && originalElement != null) {
      final PomTarget target = ((PomTargetPsiElement)element).getTarget();

      if (target instanceof DomAttributeChildDescription) {
        return generateDocForXmlAttribute((DomAttributeChildDescription)target, originalElement);
      }
    }

    if (element instanceof MyDocElement) {
      return ((MyDocElement)element).myDocumentation;
    }
    return null;
  }

  @Nullable
  private static String getResourceDocumentation(PsiElement element, String value) {
    ResourceUrl url = ResourceUrl.parse(value);
    if (url != null) {
      return generateDoc(element, url);
    } else {
      // See if it's in a resource file definition: This allows you to invoke
      // documentation on <string name="cursor_here">...</string>
      // and see the various translations etc of the string
      XmlAttribute attribute = PsiTreeUtil.getParentOfType(element, XmlAttribute.class, false);
      if (attribute != null && ATTR_NAME.equals(attribute.getName())) {
        XmlTag tag = attribute.getParent();
        String typeName = tag.getName();
        if (TAG_ITEM.equals(typeName)) {
          typeName = tag.getAttributeValue(ATTR_TYPE);
          if (typeName == null) {
            return null;
          }
        }
        ResourceType type = ResourceType.getEnum(typeName);
        if (type != null) {
          return generateDoc(element, type, value, false);
        }
      }
    }
    return null;
  }

  @Nullable
  private static String generateDocForXmlAttribute(@NotNull DomAttributeChildDescription description, @NotNull PsiElement originalElement) {
    final AndroidFacet facet = AndroidFacet.getInstance(originalElement);

    if (facet == null) {
      return null;
    }
    final XmlName xmlName = description.getXmlName();
    final String localName = xmlName.getLocalName();
    String namespace = xmlName.getNamespaceKey();

    if (namespace == null) {
      return null;
    }
    if (AndroidUtils.NAMESPACE_KEY.equals(namespace)) {
      namespace = SdkConstants.NS_RESOURCES;
    }

    if (namespace.startsWith(AndroidDomExtender.ANDROID_NS_PREFIX)) {
      final Pair<AttributeDefinition, String> pair = findAttributeDefinition(originalElement, facet, namespace, localName);

      if (pair != null) {
        return generateDocForXmlAttribute(pair.getFirst(), pair.getSecond());
      }
    }
    return null;
  }

  @Nullable
  private static Pair<AttributeDefinition, String> findAttributeDefinition(@NotNull PsiElement context,
                                                                           @NotNull AndroidFacet facet,
                                                                           @NotNull final String namespace,
                                                                           @NotNull final String localName) {
    final XmlTag parentTag = PsiTreeUtil.getParentOfType(context, XmlTag.class);

    if (parentTag == null) {
      return null;
    }
    final DomElement parentDomElement = DomManager.getDomManager(parentTag.getProject()).getDomElement(parentTag);

    if (!(parentDomElement instanceof AndroidDomElement)) {
      return null;
    }
    final Ref<Pair<AttributeDefinition, String>> result = Ref.create();
    AndroidDomExtender.processAttrsAndSubtags((AndroidDomElement)parentDomElement, new AndroidDomExtender.MyCallback() {

      @Nullable
      @Override
      DomExtension processAttribute(@NotNull XmlName xn, @NotNull AttributeDefinition attrDef, @Nullable String parentStyleableName) {
        if (xn.getLocalName().equals(localName) &&
            namespace.equals(xn.getNamespaceKey())) {
          result.set(Pair.of(attrDef, parentStyleableName));
          stop();
        }
        return null;
      }
    }, facet, false, true);

    final Pair<AttributeDefinition, String> pair = result.get();

    if (pair != null) {
      return pair;
    }
    final AttributeDefinition attrDef = findAttributeDefinitionGlobally(facet, namespace, localName);
    return attrDef != null ? Pair.of(attrDef, (String)null) : null;
  }

  @Nullable
  private static AttributeDefinition findAttributeDefinitionGlobally(@NotNull AndroidFacet facet,
                                                                     @NotNull String namespace,
                                                                     @NotNull String localName) {
    ResourceManager resourceManager = null;

    if (namespace.startsWith(AndroidDomExtender.ANDROID_NS_PREFIX)) {
      if (SdkConstants.NS_RESOURCES.equals(namespace)) {
        resourceManager = facet.getSystemResourceManager();
      }
      else if (namespace.equals(AndroidDomExtender.getNamespaceKeyByResourcePackage(facet, null))) {
        resourceManager = facet.getLocalResourceManager();
      }
    }
    else {
      resourceManager = facet.getSystemResourceManager();
    }

    if (resourceManager != null) {
      final AttributeDefinitions attrDefs = resourceManager.getAttributeDefinitions();

      if (attrDefs != null) {
        return attrDefs.getAttrDefByName(localName);
      }
    }
    return null;
  }

  private static String generateDocForXmlAttribute(@NotNull AttributeDefinition definition, @Nullable String parentStyleable) {
    final StringBuilder builder = new StringBuilder("<html><body>");
    final Set<AttributeFormat> formats = definition.getFormats();

    if (formats.size() > 0) {
      builder.append("Formats: ");
      final List<String> formatLabels = new ArrayList<String>(formats.size());

      for (AttributeFormat format : formats) {
        formatLabels.add(format.name().toLowerCase());
      }
      Collections.sort(formatLabels);

      for (int i = 0, n = formatLabels.size(); i < n; i++) {
        builder.append(formatLabels.get(i));

        if (i < n - 1) {
          builder.append(", ");
        }
      }
    }
    final String[] values = definition.getValues();

    if (values.length > 0) {
      if (builder.length() > 0) {
        builder.append("<br>");
      }
      builder.append("Values: ");
      final String[] sortedValues = new String[values.length];
      System.arraycopy(values, 0, sortedValues, 0, values.length);
      Arrays.sort(sortedValues);

      for (int i = 0; i < sortedValues.length; i++) {
        builder.append(sortedValues[i]);

        if (i < sortedValues.length - 1) {
          builder.append(", ");
        }
      }
    }
    final String docValue = definition.getDocValue(parentStyleable);

    if (docValue != null && docValue.length() > 0) {
      if (builder.length() > 0) {
        builder.append("<br><br>");
      }
      builder.append(docValue);
    }
    builder.append("</body></html>");
    return builder.toString();
  }

  @Nullable
  private static String generateDoc(PsiElement originalElement, ResourceType type, String name, boolean framework) {
    Module module = ModuleUtilCore.findModuleForPsiElement(originalElement);
    if (module == null) {
      return null;
    }

    return AndroidJavaDocRenderer.render(module, type, name, framework);
  }

  @Nullable
  private static String generateDoc(PsiElement originalElement, ResourceUrl url) {
    Module module = ModuleUtilCore.findModuleForPsiElement(originalElement);
    if (module == null) {
      return null;
    }

    return AndroidJavaDocRenderer.render(module, url);
  }

  @Override
  public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
    if (!(element instanceof XmlAttributeValue) || !(object instanceof String)) {
      return null;
    }
    final String value = (String)object;
    final PsiElement parent = element.getParent();

    if (!(parent instanceof XmlAttribute)) {
      return null;
    }
    final GenericAttributeValue domValue = DomManager.getDomManager(
      parent.getProject()).getDomElement((XmlAttribute)parent);

    if (domValue == null) {
      return null;
    }
    final Converter converter = domValue.getConverter();

    if (converter instanceof AttributeValueDocumentationProvider) {
      final String doc = ((AttributeValueDocumentationProvider)converter).getDocumentation(value);

      if (doc != null) {
        return new MyDocElement(element, doc);
      }
    }

    if (value.startsWith(PREFIX_RESOURCE_REF) || value.startsWith(PREFIX_THEME_REF)) {
      return new MyResourceElement(element, value);
    }

    return null;
  }

  @Override
  public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
    return null;
  }

  private static class MyDocElement extends FakePsiElement {
    final PsiElement myParent;
    final String myDocumentation;

    private MyDocElement(@NotNull PsiElement parent, @NotNull String documentation) {
      myParent = parent;
      myDocumentation = documentation;
    }

    @Override
    public PsiElement getParent() {
      return myParent;
    }
  }

  private static class MyResourceElement extends FakePsiElement {
    final PsiElement myParent;
    final String myResource;

    private MyResourceElement(@NotNull PsiElement parent, @NotNull String resource) {
      myParent = parent;
      myResource = resource;
    }

    @Override
    public PsiElement getParent() {
      return myParent;
    }
  }
}
