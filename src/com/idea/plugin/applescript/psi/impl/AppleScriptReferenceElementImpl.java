package com.idea.plugin.applescript.psi.impl;

import com.idea.plugin.applescript.lang.AppleScriptComponentType;
import com.idea.plugin.applescript.lang.resolve.AppleScriptComponentScopeResolver;
import com.idea.plugin.applescript.lang.resolve.AppleScriptDictionaryResolveProcessor;
import com.idea.plugin.applescript.lang.resolve.AppleScriptResolveUtil;
import com.idea.plugin.applescript.lang.resolve.AppleScriptResolver;
import com.idea.plugin.applescript.lang.sdef.DictionaryComponent;
import com.idea.plugin.applescript.lang.util.ScopeUtil;
import com.idea.plugin.applescript.psi.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.UnfairTextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Andrey on 13.04.2015.
 */
public class AppleScriptReferenceElementImpl extends AppleScriptExpressionImpl implements
        AppleScriptReferenceElement, PsiPolyVariantReference {


  public AppleScriptReferenceElementImpl(ASTNode node) {
    super(node);
    //todo create private field - myElement and do not extent wrapper??
  }

  @NotNull
  @Override
  public ResolveResult[] multiResolve(boolean incompleteCode) {
    ResolveResult[] results = multiResolveInner(incompleteCode);
    return results;//.length > 1 ? new ResolveResult[]{results[0]} : results;
  }

  protected ResolveResult[] multiResolveInner(boolean incompleteCode) {
    final List<? extends PsiElement> elements =
            ResolveCache.getInstance(getProject()).resolveWithCaching(this, AppleScriptResolver.INSTANCE,
                    true, incompleteCode);
    return AppleScriptResolveUtil.toCandidateInfoArray(elements);
  }

  @Override
  public PsiReference getReference() {
    return this;
  }

  @Override
  public PsiElement getElement() {
    return this;
  }

  @Override
  public TextRange getRangeInElement() {
    final TextRange textRange = getTextRange();

    AppleScriptReferenceElement[] appleScriptReferences = PsiTreeUtil.getChildrenOfType(this,
            AppleScriptReferenceElement.class);
    if (appleScriptReferences != null && appleScriptReferences.length > 0) {
      TextRange lastReferenceRange = appleScriptReferences[appleScriptReferences.length - 1].getTextRange();
      return new UnfairTextRange(
              lastReferenceRange.getStartOffset() - textRange.getStartOffset(),
              lastReferenceRange.getEndOffset() - textRange.getEndOffset()
      );
    }
    return new UnfairTextRange(0, textRange.getEndOffset() - textRange.getStartOffset());
  }

  @Nullable
  @Override
  public PsiElement resolve() {
    final ResolveResult[] resolveResults = multiResolve(true);

    return resolveResults.length == 0 || resolveResults.length > 0 &&
            !resolveResults[0].isValidResult() ? null : resolveResults[0].getElement();
  }

  @NotNull
  @Override
  public String getCanonicalText() {
    return getText();
  }

  @Override
  public PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
    PsiElement element = this;
    final AppleScriptIdentifier identifier = PsiTreeUtil.getChildOfType(element, AppleScriptIdentifier.class);
    final AppleScriptIdentifier identifierNew = AppleScriptPsiElementFactory.createIdentifierFromText(getProject(),
            newElementName);
    if (identifierNew != null && identifier != null) {
      element.getNode().replaceChild(identifier.getNode(), identifierNew.getNode());
    }
    return this;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    return this;
  }

  @Override
  public boolean isReferenceTo(PsiElement element) {
    PsiElement target = resolve();
    if (target instanceof AppleScriptTargetVariable) {//could be defined more than once
      final String theirName = ((AppleScriptTargetVariable) target).getName();
      String ourName = getCanonicalText();
      if (ourName.equals(theirName)) {
        final PsiElement ourScopeOwner = ScopeUtil.getMaxLocalScopeForTargetOrReference(getElement());
        final PsiElement theirScopeOwner = ScopeUtil.getMaxLocalScopeForTargetOrReference(element);
        if (resolvesToSameLocal(element, theirName, ourScopeOwner, theirScopeOwner)) {
          return true;
        }
      }
    }
    return target == element;
  }

  private boolean resolvesToSameLocal(PsiElement element, String name, PsiElement ourScopeOwner, PsiElement
          theirScopeOwner) {
    return ourScopeOwner == theirScopeOwner;
  }

  @NotNull
  @Override
  public Object[] getVariants() {
    List<? extends PsiElement> elements =
            ResolveCache.getInstance(getProject()).resolveWithCaching(this, AppleScriptComponentScopeResolver
                    .INSTANCE, true, true);

    final AppleScriptDictionaryResolveProcessor resolveProcessor =
            new AppleScriptDictionaryResolveProcessor(getProject(), getCanonicalText());
    ResolveState myCollectAllState = ResolveState.initial()
            .put(AppleScriptDictionaryResolveProcessor.COLLECT_ALL_DECLARATIONS, true);
    PsiTreeUtil.treeWalkUp(resolveProcessor, getElement(), null, myCollectAllState);
    List<DictionaryComponent> dictionaryComponents = resolveProcessor.getFilteredResult();

    List<LookupElement> lookupElements = new ArrayList<LookupElement>();
    if (elements != null && !elements.isEmpty()) {
      for (PsiElement el : elements) {
        addLookupElement(lookupElements, el);
      }
    }
    for (PsiElement el : dictionaryComponents) {
      addLookupElement(lookupElements, el);
    }
    return !lookupElements.isEmpty() ? lookupElements.toArray() : LookupElement.EMPTY_ARRAY;
  }

  private void addLookupElement(List<LookupElement> lookupElements, PsiElement el) {
    LookupElementBuilder builder;
    if (el instanceof AppleScriptComponent) {
      builder = LookupElementBuilder.createWithIcon((AppleScriptComponent) el);
    } else {
      builder = LookupElementBuilder.create(el);
    }
    AppleScriptComponentType componentType = AppleScriptComponentType.typeOf(el);
    String typeText = componentType != null ? componentType.toString().toLowerCase() : null;
    builder = builder.withTypeText(typeText, null, true);
    lookupElements.add(builder);
  }

  @Override
  public boolean isSoft() {
    return false;
  }
}
