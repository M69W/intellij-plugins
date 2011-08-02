/*
 * Copyright 2010 The authors
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.struts2.reference;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.paths.PathReferenceManager;
import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.psi.*;
import com.intellij.psi.css.impl.util.CssInHtmlClassOrIdReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.IdRefReference;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.struts2.reference.jsp.ActionPropertyReferenceProvider;
import com.intellij.struts2.reference.jsp.ActionReferenceProvider;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static com.intellij.patterns.PlatformPatterns.virtualFile;
import static com.intellij.patterns.StandardPatterns.*;
import static com.intellij.patterns.XmlPatterns.xmlAttributeValue;
import static com.intellij.patterns.XmlPatterns.xmlTag;

/**
 * Base class for Taglib reference contributors.
 *
 * @author Yann C&eacute;bron
 */
public abstract class StrutsTaglibReferenceContributorBase extends PsiReferenceContributor {

  protected static final PsiReferenceProvider CSS_CLASS_PROVIDER = new CssInHtmlClassOrIdReferenceProvider();

  protected static final PsiReferenceProvider BOOLEAN_VALUE_REFERENCE_PROVIDER =
      new StaticStringValuesReferenceProvider(false, "false", "true");

  protected static final PsiReferenceProvider ACTION_REFERENCE_PROVIDER = new ActionReferenceProvider();

  protected static final PsiReferenceProvider ACTION_PROPERTY_REFERENCE_PROVIDER =
      new ActionPropertyReferenceProvider(false);
  protected static final PsiReferenceProvider ACTION_READONLY_PROPERTY_REFERENCE_PROVIDER =
      new ActionPropertyReferenceProvider(true);

  protected static final PsiReferenceProvider RELATIVE_PATH_PROVIDER = new PsiReferenceProvider() {
    @NotNull
    public PsiReference[] getReferencesByElement(@NotNull final PsiElement element,
                                                 @NotNull final ProcessingContext context) {
      final String pathValue = ((XmlAttributeValue) element).getValue();
      return PathReferenceManager.getInstance()
                                 .createReferences(element, TaglibUtil.isDynamicExpression(pathValue), false, true);
    }
  };

  /**
   * Reference to HTML element's "id".
   */
  protected static final PsiReferenceProvider HTML_ID_REFERENCE_PROVIDER = new PsiReferenceProvider() {
    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull final PsiElement element,
                                                 @NotNull final ProcessingContext context) {
      return new PsiReference[]{new IdRefReference(element)};
    }
  };

  /**
   * Wrapped .properties key reference (disable in facet settings).
   */
  protected final PsiReferenceProvider wrappedPropertiesProvider = new WrappedPropertiesReferenceProvider();

  /**
   * Reference to HTML element's "id" with additional pseudo-IDs.
   */
  protected static class HtmlIdWithAdditionalVariantsReferenceProvider extends PsiReferenceProvider {

    private final String[] additionalVariants;

    protected HtmlIdWithAdditionalVariantsReferenceProvider(final String... additionalVariants) {
      Arrays.sort(additionalVariants);
      this.additionalVariants = additionalVariants;
    }

    @NotNull
    @Override
    public PsiReference[] getReferencesByElement(@NotNull final PsiElement element,
                                                 @NotNull final ProcessingContext context) {
      return new PsiReference[]{new IdRefReference(element) {
        @Override
        public PsiElement resolve() {
          final PsiElement resolve = super.resolve();
          if (resolve != null) {
            return resolve;
          }

          return Arrays.binarySearch(additionalVariants,
                                     ((XmlAttributeValue) myElement).getValue()) > -1 ? myElement : null;
        }

        @NotNull
        @Override
        public Object[] getVariants() {
          return ArrayUtil.mergeArrays(super.getVariants(), additionalVariants, Object.class);
        }
      }};
    }
  }

  /**
   * Element pattern for accessing taglib in JSPs.
   */
  private final XmlAttributeValuePattern jspElementPattern =
      xmlAttributeValue()
          .inVirtualFile(or(virtualFile().ofType(StdFileTypes.JSP),
                            virtualFile().ofType(StdFileTypes.JSPX)))
          .withSuperParent(2, xmlTag().withNamespace(getNamespace()));

  /**
   * Returns the taglib's namespace.
   *
   * @return Namespace.
   */
  @NonNls
  @NotNull
  protected abstract String getNamespace();

  /**
   * Register the given provider on the given attribute/tag(s) combination(s).
   *
   * @param provider      Provider to install.
   * @param attributeName Attribute name.
   * @param registrar     Registrar instance.
   * @param tagNames      Tag name(s).
   */
  protected void registerTags(final PsiReferenceProvider provider,
                              @NonNls final String attributeName,
                              final PsiReferenceRegistrar registrar,
                              @NonNls final String... tagNames) {
    registrar.registerReferenceProvider(
        and(
            xmlAttributeValue()
                .withLocalName(attributeName)
                .withSuperParent(2, xmlTag().withLocalName(string().oneOf(tagNames))),
            jspElementPattern
           ),
        provider);
  }

  /**
   * Registers a boolean value (true/false) provider on the given tag(s)/attribute-combination(s).
   *
   * @param attributeName Attribute name.
   * @param registrar     Registrar instance.
   * @param tagNames      Tag name(s).
   */
  protected void registerBoolean(@NonNls final String attributeName,
                                 final PsiReferenceRegistrar registrar,
                                 @NonNls final String... tagNames) {
    registerTags(BOOLEAN_VALUE_REFERENCE_PROVIDER,
                 attributeName,
                 registrar,
                 tagNames);
  }

}