/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij;

import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Class ASTUtils - used for AST related util functionalities
 */
public class ASTUtils {
    /**
     * Method used to fetch all method declarations
     * @param unit the PSI Java file
     * @return collection of all method declarations
     */
    public static @NotNull Collection<PsiMethod> getAllMethodDeclarations(PsiJavaFile unit) {
        Collection<PsiMethod> allMethodDeclarations = PsiTreeUtil.findChildrenOfType(unit, PsiMethod.class);
        return allMethodDeclarations;
    }

    /**
     * Find a PSI element (field, method, or parameter) by name within a PSI file.
     *
     * @param psiFile the PSI Java file
     * @param elementName the name of the element to find
     * @param elementType the type of element (PsiField.class, PsiMethod.class, PsiParameter.class)
     * @param <T> the type of PSI element to find
     * @return the found PSI element or null
     */
    @Nullable
    public static <T extends PsiElement> T findPsiElement(PsiJavaFile psiFile, String elementName, Class<T> elementType) {
        if (psiFile == null || elementName == null || elementName.isEmpty()) {
            return null;
        }

        // Use PsiTreeUtil to find elements
        Collection<T> elements = PsiTreeUtil.findChildrenOfType(psiFile, elementType);
        for (T element : elements) {
            if (element instanceof PsiNamedElement) {
                if (elementName.equals(((PsiNamedElement) element).getName())) {
                    return element;
                }
            }
        }
        return null;
    }

    /**
     * Extract the type from a PSI element (field, method return type, or parameter type).
     *
     * @param element the PSI element
     * @return the PsiType or null
     */
    @Nullable
    public static PsiType getTypeFromPsiElement(PsiElement element) {
        if (element instanceof PsiField) {
            return ((PsiField) element).getType();
        } else if (element instanceof PsiMethod) {
            return ((PsiMethod) element).getReturnType();
        } else if (element instanceof PsiParameter) {
            return ((PsiParameter) element).getType();
        }
        return null;
    }
}
