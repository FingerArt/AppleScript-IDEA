package com.intellij.plugin.applescript.lang.formatter;

import com.intellij.formatting.Spacing;
import com.intellij.lang.ASTNode;
import com.intellij.plugin.applescript.psi.impl.AppleScriptPsiImplUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.impl.source.SourceTreeToPsiMap;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.intellij.plugin.applescript.psi.AppleScriptTokenTypesSets.KEYWORDS;
import static com.intellij.plugin.applescript.psi.AppleScriptTypes.*;

/**
 * Andrey 08.04.2015
 */
public class AppleScriptSpacingProcessor {

  private final ASTNode myNode;
  private final CommonCodeStyleSettings mySettings;

  private ASTNode myChild1;
  private ASTNode myChild2;
  private IElementType myType1;
  private IElementType myType2;
  private PsiElement myParent;


  public AppleScriptSpacingProcessor(ASTNode node, CommonCodeStyleSettings commonSettings) {
    myNode = node;
    mySettings = commonSettings;
  }

  private static boolean isWhiteSpace(final ASTNode node) {
    return node != null && (AppleScriptPsiImplUtil.isWhiteSpaceOrNls(node) || node.getTextLength() == 0);
  }

  private void _init(@Nullable final ASTNode child) {
    if (child == null) return;

    ASTNode treePrev = child.getTreePrev();
    while (treePrev != null && isWhiteSpace(treePrev)) {
      treePrev = treePrev.getTreePrev();
    }

    if (treePrev == null) {
      _init(child.getTreeParent());
    } else {
      myChild2 = child;
      myType2 = myChild2.getElementType();

      myChild1 = treePrev;
      myType1 = myChild1.getElementType();
      final CompositeElement parent = (CompositeElement) treePrev.getTreeParent();
      myParent = SourceTreeToPsiMap.treeElementToPsi(parent);
    }
  }

  Spacing getSpacing(@Nullable AppleScriptBlock child1, @NotNull AppleScriptBlock child2) {
    if (child1 == null) {
      return null;
    }
    final ASTNode node = child2.getNode();
    _init(node);

    final ASTNode node1 = child1.getNode();
    final IElementType type1 = node1.getElementType();
    final ASTNode node2 = child2.getNode();
    final IElementType parent2 = node2.getTreeParent().getElementType();
    final IElementType type2 = node2.getElementType();

    if (LCURLY == type1 || RCURLY == type2) return Spacing.createSpacing(0, 0, 0, true, 0);

    // handlerCall(params)
    if (LPAREN == type2 && HANDLER_POSITIONAL_PARAMETERS_CALL_EXPRESSION == parent2) {
      return addSingleSpaceIf(mySettings.SPACE_BEFORE_METHOD_CALL_PARENTHESES, false);
    }
    if (LPAREN == type2 && HANDLER_POSITIONAL_PARAMETERS_DEFINITION == parent2) {
      return addSingleSpaceIf(mySettings.SPACE_BEFORE_METHOD_PARENTHESES, false);
    }

    if (IF == type1 && LPAREN == type2) {
      return addSingleSpaceIf(mySettings.SPACE_BEFORE_IF_PARENTHESES, false);
    }

    if (LPAREN == type1 || RPAREN == type2) return Spacing.createSpacing(0, 0, 0, true, 0);

    if (COMMA == type2) return Spacing.createSpacing(0, 0, 0, true, 0);

    if (type1 == IDENTIFIER && type2 == HANDLER_PARAMETER_LABEL) {
      return Spacing.createSpacing(1, 1, 0, true, 0);
    }

    if (type2 == COLON) return Spacing.createSpacing(0, 0, 0, true, 0);

    if ((KEYWORDS.contains(type1) || HANDLER_PARAMETER_LABEL == type1) && NLS != type2) {
      return Spacing.createSpacing(1, 1, 0, true, 0);
    }
    if (BAND == type1 || BAND == type2) {
      return Spacing.createSpacing(1, 1, 0, true, 0);
    }

    return null;
  }

  private Spacing addLineBreak() {
    return Spacing.createSpacing(0, 0, 1, false, mySettings.KEEP_BLANK_LINES_IN_CODE);
  }

  private Spacing addSingleSpaceIf(boolean condition, boolean linesFeed) {
    final int spaces = condition ? 1 : 0;
    final int lines = linesFeed ? 1 : 0;
    return Spacing.createSpacing(spaces, spaces, lines, mySettings.KEEP_LINE_BREAKS, mySettings.KEEP_BLANK_LINES_IN_CODE);
  }
}
