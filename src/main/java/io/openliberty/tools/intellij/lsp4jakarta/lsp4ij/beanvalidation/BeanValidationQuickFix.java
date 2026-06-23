/*******************************************************************************
 * Copyright (c) 2021, 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.ModifyModifiersProposal;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.codeAction.proposal.RemoveAnnotationsProposal;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.IJavaCodeActionParticipant;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionContext;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.java.codeaction.JavaCodeActionResolveContext;
import io.openliberty.tools.intellij.util.ExceptionUtil;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;

import java.util.*;
import java.util.logging.Logger;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;

/**
 * Quickfix for fixing {@link BeanValidationConstants#DIAGNOSTIC_CODE_STATIC} error by either action
 * 1. Removing constraint annotation on static field or method
 * 2. Removing static modifier from field or method
 *
 * @author Leslie Dawson (lamminade)
 */
public class BeanValidationQuickFix implements IJavaCodeActionParticipant {

    private static final Logger LOGGER = Logger.getLogger(BeanValidationQuickFix.class.getName());

    private static final String ANNOTATION_NAME = "annotationName";

    @Override
    public String getParticipantId() {
        return BeanValidationQuickFix.class.getName();
    }

    public List<? extends CodeAction> getCodeActions(JavaCodeActionContext context, Diagnostic diagnostic) {
        List<CodeAction> codeActions = new ArrayList<>();
        removeConstraintAnnotationsCodeActions(diagnostic, context, codeActions);

        if (diagnostic.getCode().getLeft().equals(BeanValidationConstants.DIAGNOSTIC_CODE_STATIC)) {
            removeStaticModifierCodeActions(diagnostic, context, codeActions);
        }
        return codeActions;
    }

    @Override
    public CodeAction resolveCodeAction(JavaCodeActionResolveContext context) {

        final CodeAction toResolve = context.getUnresolved();

        String message = toResolve.getTitle();

        if (message.equals(Messages.getMessage("RemoveStaticModifier"))) {
            resolveStaticModifierCodeAction(context);
            return toResolve;
        }

        resolveRemoveConstraintAnnotationsCodeAction(context);

        return toResolve;
    }

    private void removeConstraintAnnotationsCodeActions(Diagnostic diagnostic, JavaCodeActionContext context, List<CodeAction> codeActions) {

        final String annotationName = diagnostic.getData().toString().replace("\"", "");
        final String name = Messages.getMessage("RemoveConstraintAnnotation", getSimpleName(annotationName));
        Map<String, Object> extendedData = new HashMap<>();
        extendedData.put(ANNOTATION_NAME, annotationName);
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId(), extendedData));
    }

    private void resolveRemoveConstraintAnnotationsCodeAction(JavaCodeActionResolveContext context) {

        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        CodeActionResolveData data = (CodeActionResolveData) toResolve.getData();
        String annotationName;
        if (data.getExtendedDataEntry(ANNOTATION_NAME) instanceof String) {
            annotationName = (String) data.getExtendedDataEntry(ANNOTATION_NAME);
        } else {
            annotationName = "";
        }
        final String name = toResolve.getTitle();
        final PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);
        
        // First, try to find annotation in modifier list (field/method/parameter-level annotations)
        final PsiAnnotation[] annotations = modifierListOwner.getAnnotations();
        PsiAnnotation annotationToRemove = null;

        if (annotations != null && annotations.length > 0) {
            final Optional<PsiAnnotation> foundAnnotation =
                    Arrays.stream(annotations).filter(a -> annotationName.equals(a.getQualifiedName())).findFirst();
            if (foundAnnotation.isPresent()) {
                annotationToRemove = foundAnnotation.get();
            }
        }
        
        // If not found in modifiers, search for TYPE_USE annotations within type structures
        if (annotationToRemove == null) {
            annotationToRemove = findTypeUseAnnotation(modifierListOwner, annotationName);
        }

        if (annotationToRemove != null) {
            boolean isFormatRequired = false;
            final RemoveAnnotationsProposal proposal = new RemoveAnnotationsProposal(name, context.getSource().getCompilationUnit(),
                    context.getASTRoot(), parentType, 0, Collections.singletonList(annotationToRemove), isFormatRequired);

            ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action to remove constraint annotation");
        }
    }
    
    /**
     * Find TYPE_USE annotation within type structures (generics, arrays, etc.)
     *
     * @param modifierListOwner the field, method, or parameter
     * @param annotationName the fully qualified annotation name to find
     * @return the PsiAnnotation if found, null otherwise
     */
    private PsiAnnotation findTypeUseAnnotation(PsiModifierListOwner modifierListOwner, String annotationName) {
        PsiTypeElement typeElement = null;
        
        // Get the type element based on the element type
        if (modifierListOwner instanceof PsiField) {
            typeElement = ((PsiField) modifierListOwner).getTypeElement();
        } else if (modifierListOwner instanceof PsiMethod) {
            typeElement = ((PsiMethod) modifierListOwner).getReturnTypeElement();
        } else if (modifierListOwner instanceof PsiParameter) {
            typeElement = ((PsiParameter) modifierListOwner).getTypeElement();
        }
        
        if (typeElement == null) {
            return null;
        }
        
        // Recursively search for the annotation in the type structure
        return findAnnotationInTypeElement(typeElement, annotationName);
    }
    
    /**
     * Recursively search for an annotation within a type element structure
     *
     * @param typeElement the type element to search
     * @param annotationName the fully qualified annotation name to find
     * @return the PsiAnnotation if found, null otherwise
     */
    private PsiAnnotation findAnnotationInTypeElement(PsiTypeElement typeElement, String annotationName) {
        if (typeElement == null) {
            return null;
        }
        
        // Check annotations on this type element
        PsiAnnotation[] annotations = typeElement.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            if (annotationName.equals(annotation.getQualifiedName())) {
                return annotation;
            }
        }
        
        // Recursively search all descendant type elements (not just direct children)
        // This handles nested generics like Map<String, List<@Email Integer>>
        PsiElement[] allElements = PsiTreeUtil.collectElements(typeElement,
            element -> element instanceof PsiTypeElement && element != typeElement);
        
        for (PsiElement element : allElements) {
            if (element instanceof PsiTypeElement) {
                PsiTypeElement childTypeElement = (PsiTypeElement) element;
                PsiAnnotation[] childAnnotations = childTypeElement.getAnnotations();
                for (PsiAnnotation annotation : childAnnotations) {
                    if (annotationName.equals(annotation.getQualifiedName())) {
                        return annotation;
                    }
                }
            }
        }
        
        return null;
    }

    private void resolveStaticModifierCodeAction(JavaCodeActionResolveContext context) {
        final CodeAction toResolve = context.getUnresolved();
        final PsiElement node = context.getCoveredNode();
        final PsiClass parentType = PsiTreeUtil.getParentOfType(node, PsiClass.class);
        final PsiModifierListOwner modifierListOwner = PsiTreeUtil.getParentOfType(node, PsiModifierListOwner.class);

        final ModifyModifiersProposal proposal = new ModifyModifiersProposal(toResolve.getTitle()
                , context.getSource().getCompilationUnit(),
                context.getASTRoot(), parentType, 0, modifierListOwner.getModifierList(), Collections.emptyList(),
                Collections.singletonList("static"));

        ExceptionUtil.executeWithWorkspaceEditHandling(context, proposal, toResolve, LOGGER, "Unable to create workspace edit for code action to remove static modifier");
    }

    private void removeStaticModifierCodeActions(Diagnostic diagnostic, JavaCodeActionContext context,
                                                    List<CodeAction> codeActions) {

        final String name = Messages.getMessage("RemoveStaticModifier");
        codeActions.add(JDTUtils.createCodeAction(context, diagnostic, name, getParticipantId()));
    }

}


