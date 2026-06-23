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
package io.openliberty.tools.intellij.lsp4jakarta.it.beanvalidation;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import io.openliberty.tools.intellij.lsp4jakarta.it.core.BaseJakartaTest;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.IPsiUtils;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.internal.core.ls.PsiUtilsLSImpl;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4jakarta.commons.JakartaJavaDiagnosticsParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.util.Arrays;

import static io.openliberty.tools.intellij.lsp4jakarta.it.core.JakartaForJavaAssert.*;

/**
 * JUnit test class for TYPE_USE validation of Bean Validation constraints.
 * Tests validation of constraints on generic type arguments and array component types.
 */
@RunWith(JUnit4.class)
public class BeanValidationTypeUseTest extends BaseJakartaTest {

    @Test
    public void testStringConstraintsOnGenerics() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/beanvalidation/TypeUseStringConstraints.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 26: List<@Email Integer> - @Email on Integer (should be String/CharSequence)
        Diagnostic emailOnIntegerDiagnostic = d(25, 12, 32,
                "The @Email annotation can only be used on String and CharSequence type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Email");

        // Line 29: List<@NotBlank Integer> - @NotBlank on Integer (should be String/CharSequence)
        Diagnostic notBlankOnIntegerDiagnostic = d(28, 12, 35,
                "The @NotBlank annotation can only be used on String and CharSequence type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.NotBlank");

        // Line 32: List<@Pattern Boolean> - @Pattern on Boolean (should be String/CharSequence)
        Diagnostic patternOnBooleanDiagnostic = d(31, 12, 49,
                "The @Pattern annotation can only be used on String and CharSequence type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Pattern");

        assertJavaDiagnostics(diagnosticsParams, utils,
                emailOnIntegerDiagnostic, notBlankOnIntegerDiagnostic, patternOnBooleanDiagnostic);
    }

    @Test
    public void testBooleanConstraintsOnGenerics() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/beanvalidation/TypeUseBooleanConstraints.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 23: List<@AssertTrue String> - @AssertTrue on String (should be boolean/Boolean)
        Diagnostic assertTrueOnStringDiagnostic = d(22, 12, 36,
                "The @AssertTrue annotation can only be used on boolean and Boolean type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.AssertTrue");

        // Line 26: List<@AssertFalse Integer> - @AssertFalse on Integer (should be boolean/Boolean)
        Diagnostic assertFalseOnIntegerDiagnostic = d(25, 12, 38,
                "The @AssertFalse annotation can only be used on boolean and Boolean type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.AssertFalse");

        assertJavaDiagnostics(diagnosticsParams, utils,
                assertTrueOnStringDiagnostic, assertFalseOnIntegerDiagnostic);
    }

    @Test
    public void testNumericConstraintsOnGenerics() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/beanvalidation/TypeUseNumericConstraints.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 29: List<@Min(1) String> - @Min on String (should be numeric)
        Diagnostic minOnStringDiagnostic = d(28, 12, 32,
                "The @Min annotation can only be used on \n" +
                        "- BigDecimal \n" +
                        "- BigInteger\n" +
                        "- byte, short, int, long (and their respective wrappers) \n" +
                        " type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Min");

        // Line 32: List<@Max(100) Boolean> - @Max on Boolean (should be numeric)
        Diagnostic maxOnBooleanDiagnostic = d(31, 12, 35,
                "The @Max annotation can only be used on \n" +
                        "- BigDecimal \n" +
                        "- BigInteger\n" +
                        "- byte, short, int, long (and their respective wrappers) \n" +
                        " type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Max");

        // Line 35: List<@Positive String> - @Positive on String (should be numeric)
        Diagnostic positiveOnStringDiagnostic = d(34, 12, 34,
                "The @Positive annotation can only be used on \n" +
                        "- BigDecimal \n" +
                        "- BigInteger\n" +
                        "- byte, short, int, long, float, double (and their respective wrappers) \n" +
                        " type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Positive");

        // Line 38: List<@Negative Boolean> - @Negative on Boolean (should be numeric)
        Diagnostic negativeOnBooleanDiagnostic = d(37, 12, 35,
                "The @Negative annotation can only be used on \n" +
                        "- BigDecimal \n" +
                        "- BigInteger\n" +
                        "- byte, short, int, long, float, double (and their respective wrappers) \n" +
                        " type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Negative");

        assertJavaDiagnostics(diagnosticsParams, utils,
                minOnStringDiagnostic, maxOnBooleanDiagnostic,
                positiveOnStringDiagnostic, negativeOnBooleanDiagnostic);
    }

    @Test
    public void testMethodConstraints() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/beanvalidation/TypeUseMethodConstraints.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 27: public List<@Email Integer> getInvalidEmails() - return type (TYPE_USE in generic)
        Diagnostic emailOnGenericReturnTypeDiagnostic = d(26, 11, 31,
                "The @Email annotation can only be used on String and CharSequence type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Email");

        // Line 32: public @Email Integer[] getInvalidEmailArray() - return type array
        Diagnostic emailOnArrayReturnTypeDiagnostic = d(31, 28, 48,
                "The @Email annotation can only be used on String and CharSequence type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Email");

        // Line 49: public void setInvalidEmails(List<@Email Integer> numbers) - parameter (TYPE_USE in generic)
        Diagnostic emailOnGenericParameterDiagnostic = d(48, 39, 59,
                "The @Email annotation can only be used on String and CharSequence type parameters.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Email");

        // Line 53: public void setInvalidEmailArray(@Email Integer[] numbers) - parameter array
        Diagnostic emailOnArrayParameterDiagnostic = d(52, 54, 61,
                "The @Email annotation can only be used on String and CharSequence type parameters.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Email");

        assertJavaDiagnostics(diagnosticsParams, utils,
                emailOnGenericReturnTypeDiagnostic, emailOnArrayReturnTypeDiagnostic,
                emailOnGenericParameterDiagnostic, emailOnArrayParameterDiagnostic);
    }

    @Test
    public void testComplexNestedGenerics() throws Exception {
        Module module = createMavenModule(new File("src/test/resources/projects/maven/jakarta-sample"));
        IPsiUtils utils = PsiUtilsLSImpl.getInstance(getProject());

        VirtualFile javaFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(ModuleUtilCore.getModuleDirPath(module)
                + "/src/main/java/io/openliberty/sample/jakarta/beanvalidation/TypeUseComplexConstraints.java");
        String uri = VfsUtilCore.virtualToIoFile(javaFile).toURI().toString();

        JakartaJavaDiagnosticsParams diagnosticsParams = new JakartaJavaDiagnosticsParams();
        diagnosticsParams.setUris(Arrays.asList(uri));

        // Line 24: Map<String, List<@Email Integer>> - @Email on Integer in nested generic
        Diagnostic emailOnMapValueTypeDiagnostic = d(23, 12, 45,
                "The @Email annotation can only be used on String and CharSequence type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.Email");

        // Line 27: Map<@NotBlank Integer, List<String>> - @NotBlank on Integer in nested generic
        Diagnostic notBlankOnMapKeyTypeDiagnostic = d(26, 12, 48,
                "The @NotBlank annotation can only be used on String and CharSequence type methods.",
                DiagnosticSeverity.Error, "jakarta-bean-validation",
                "FixTypeOfElement", "jakarta.validation.constraints.NotBlank");

        assertJavaDiagnostics(diagnosticsParams, utils,
                emailOnMapValueTypeDiagnostic, notBlankOnMapKeyTypeDiagnostic);
    }
}
