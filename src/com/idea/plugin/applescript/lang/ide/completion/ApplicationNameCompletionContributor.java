package com.idea.plugin.applescript.lang.ide.completion;

import com.idea.plugin.applescript.lang.ide.sdef.AppleScriptSystemDictionaryRegistry;
import com.idea.plugin.applescript.psi.AppleScriptApplicationReference;
import com.idea.plugin.applescript.psi.AppleScriptUseStatement;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * Created by Andrey on 15.10.2015.
 */
public class ApplicationNameCompletionContributor extends CompletionContributor {

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    super.fillCompletionVariants(parameters, result);
  }

  public ApplicationNameCompletionContributor() {
    final PsiElementPattern.Capture<AppleScriptApplicationReference> inApplicationReferenceString =
            psiElement(AppleScriptApplicationReference.class);//.withParent(AppleScriptApplicationReference.class);
    final PsiElementPattern.Capture<PsiElement> inAppReferenceString =
            psiElement().andOr(psiElement().withSuperParent(2, AppleScriptApplicationReference.class),
                    psiElement().withSuperParent(2, AppleScriptUseStatement.class));

    final PsiElementPattern.Capture<PsiElement> any =
            psiElement();

    extend(CompletionType.BASIC,
            inAppReferenceString,
            new CompletionProvider<CompletionParameters>() {
              @Override
              protected void addCompletions(@NotNull CompletionParameters parameters,
                                            ProcessingContext context,
                                            @NotNull CompletionResultSet result) {

                AppleScriptSystemDictionaryRegistry systemDictionaryRegistry = ApplicationManager.getApplication()
                        .getComponent(AppleScriptSystemDictionaryRegistry.class);
                if (systemDictionaryRegistry != null) {
                  Collection<String> appNameList;
                  if (SystemInfo.isMac) {
                    appNameList = systemDictionaryRegistry.getAllSystemApplicationNames();
                  } else {
                    appNameList = systemDictionaryRegistry.getCachedApplicationNames();
                  }
                  for (String appName : appNameList) {
                    result.addElement(LookupElementBuilder.create(appName));
                  }
                }
              }
            });

  }
}
