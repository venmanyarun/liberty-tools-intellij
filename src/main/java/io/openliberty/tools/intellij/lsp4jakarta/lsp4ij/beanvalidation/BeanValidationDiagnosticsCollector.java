/*******************************************************************************
 * Copyright (c) 2020, 2026 IBM Corporation, Reza Akhavan and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation, Reza Akhavan - initial API and implementation
 *******************************************************************************/

package io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.AbstractDiagnosticsCollector;
import io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.Messages;
import io.openliberty.tools.intellij.lsp4mp4ij.psi.core.utils.AnnotationUtils;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;

import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.DiagnosticsUtils.inheritsFrom;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.JDTUtils.getSimpleName;
import static io.openliberty.tools.intellij.lsp4jakarta.lsp4ij.beanvalidation.BeanValidationConstants.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BeanValidationDiagnosticsCollector extends AbstractDiagnosticsCollector {

    private static final Logger LOGGER = Logger.getLogger(BeanValidationDiagnosticsCollector.class.getName());
    public BeanValidationDiagnosticsCollector() {
        super();
    }

    @Override
    protected String getDiagnosticSource() {
        return DIAGNOSTIC_SOURCE;
    }

    public void collectDiagnostics(PsiJavaFile unit, List<Diagnostic> diagnostics) {
        if (unit != null) {
            PsiClass[] alltypes;
            PsiField[] allFields;
            PsiMethod[] allMethods;

            alltypes = unit.getClasses();
            for (PsiClass type : alltypes) {
                allFields = type.getFields();
                for (PsiField field : allFields) {
                    // NEW: Process TYPE_USE annotations with lazy check FIRST
                    PsiType fieldType = field.getType();
                    boolean hasTypeUseAnnotations = mightHaveTypeUseAnnotations(fieldType);
                    
                    if (hasTypeUseAnnotations) {
                        // Get the type element for diagnostic placement
                        PsiTypeElement typeElement = field.getTypeElement();
                        if (typeElement != null) {
                            processTypeUseAnnotations(field, fieldType, typeElement, type, diagnostics);
                        }
                    }
                    
                    // Only process direct annotations if there are no TYPE_USE annotations
                    // This prevents duplicates since TYPE_USE annotations also appear in modifierList
                    if (!hasTypeUseAnnotations) {
                        processAnnotations(field, type, diagnostics);
                    }
                }
                allMethods = type.getMethods();
                for (PsiMethod method : allMethods) {
                    // Check if method or any of its parameters have TYPE_USE annotations
                    boolean methodHasTypeUse = false;
                    
                    // NEW: Process TYPE_USE annotations on return type FIRST
                    PsiType returnType = method.getReturnType();
                    boolean hasReturnTypeUse = mightHaveTypeUseAnnotations(returnType);
                    
                    if (hasReturnTypeUse) {
                        methodHasTypeUse = true;
                        // Get the type element for diagnostic placement
                        PsiTypeElement typeElement = method.getReturnTypeElement();
                        if (typeElement != null) {
                            processTypeUseAnnotations(method, returnType, typeElement, type, diagnostics);
                        }
                    }
                    
                    // Check parameters for TYPE_USE annotations
                    PsiParameter[] parameters = method.getParameterList().getParameters();
                    for (PsiParameter parameter : parameters) {
                        // NEW: Process TYPE_USE annotations on parameter type FIRST
                        PsiType paramType = parameter.getType();
                        boolean hasParamTypeUse = mightHaveTypeUseAnnotations(paramType);
                        
                        if (hasParamTypeUse) {
                            methodHasTypeUse = true;
                            // Get the type element for diagnostic placement
                            PsiTypeElement typeElement = parameter.getTypeElement();
                            if (typeElement != null) {
                                processTypeUseAnnotations(parameter, paramType, typeElement, type, diagnostics);
                            }
                        }
                    }
                    
                    // Only process method-level annotations if no TYPE_USE annotations exist anywhere
                    // This prevents picking up TYPE_USE annotations from modifierList
                    if (!methodHasTypeUse) {
                        processAnnotations(method, type, diagnostics);
                        for (PsiParameter parameter : parameters) {
                            processAnnotations(parameter, type, diagnostics);
                        }
                    }
                }
            }
        }
    }

    private void processAnnotations(PsiJvmModifiersOwner psiModifierOwner, PsiClass type, List<Diagnostic> diagnostics) {
        // Get annotations from modifier list
        PsiModifierList modifierList = psiModifierOwner.getModifierList();
        if (modifierList == null) {
            return;
        }
        
        PsiAnnotation[] allAnnotations = modifierList.getAnnotations();
        
        // Filter out TYPE_USE annotations by checking if they're within a type element
        List<PsiAnnotation> nonTypeUseAnnotations = new ArrayList<>();
        for (PsiAnnotation annotation : allAnnotations) {
            if (!isWithinTypeElement(annotation, psiModifierOwner)) {
                nonTypeUseAnnotations.add(annotation);
            }
        }
        
        PsiAnnotation[] annotations = nonTypeUseAnnotations.toArray(new PsiAnnotation[0]);
        
        // Check for conflicting constraints
        checkConflictingConstraints(psiModifierOwner, type, annotations, diagnostics);
        
        for (PsiAnnotation annotation : annotations) {
            String matchedAnnotation = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                    SET_OF_ANNOTATIONS.toArray(new String[0]));
            if (matchedAnnotation != null) {
                validAnnotation(psiModifierOwner, annotation, matchedAnnotation, diagnostics, type);
            }
        }
    }
    
    /**
     * Check if an annotation is within a type element (i.e., it's a TYPE_USE annotation).
     * TYPE_USE annotations are those that appear on types, not on the element itself.
     */
    private boolean isWithinTypeElement(PsiAnnotation annotation, PsiJvmModifiersOwner owner) {
        PsiTypeElement typeElement = null;
        if (owner instanceof PsiField) {
            typeElement = ((PsiField) owner).getTypeElement();
        } else if (owner instanceof PsiMethod) {
            typeElement = ((PsiMethod) owner).getReturnTypeElement();
        } else if (owner instanceof PsiParameter) {
            typeElement = ((PsiParameter) owner).getTypeElement();
        }
        
        if (typeElement == null) {
            return false;
        }
        
        // Check if the annotation's text range is within the type element's text range
        // This is more reliable than tree ancestry check for annotations from modifierList
        TextRange annotationRange = annotation.getTextRange();
        TextRange typeElementRange = typeElement.getTextRange();
        
        if (annotationRange == null || typeElementRange == null) {
            return false;
        }
        
        return typeElementRange.contains(annotationRange);
    }
    

    private void validAnnotation(PsiElement element, PsiAnnotation annotation, String matchedAnnotation,
                                 List<Diagnostic> diagnostics, PsiClass classType) {
        if (element != null) {
            String annotationName = annotation.getQualifiedName();
            boolean isMethod = element instanceof PsiMethod;
            boolean isField = element instanceof PsiField;

            if (((PsiModifierListOwner)element).hasModifierProperty(PsiModifier.STATIC)) {
                String source = isMethod ?
                        Messages.getMessage("ConstraintAnnotationsMethod") :
                        Messages.getMessage("ConstraintAnnotationsField");
                diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                        source, DIAGNOSTIC_CODE_STATIC,
                        annotationName, DiagnosticSeverity.Error));
            }
            PsiType type = (isMethod) ? ((PsiMethod) element).getReturnType() : (isField) ?
                    ((PsiField) element).getType() : ((PsiParameter) element).getType();
            if (type instanceof PsiClassType) {
                PsiType t = PsiPrimitiveType.getUnboxedType(type);
                if (t != null) {
                    type = t;
                }
            }
            //The below block throws diagnostics if invalid element type is used with constraint annotations
            switch (matchedAnnotation) {
                case ASSERT_FALSE, ASSERT_TRUE -> {
                    String source = getSource(isMethod, isField, annotationName, "AnnotationBoolean");
                    if (!type.equals(PsiTypes.booleanType())) {
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case DECIMAL_MAX, DECIMAL_MIN, DIGITS -> {
                    if (!type.getCanonicalText().equals(BIG_DECIMAL)
                            && !type.getCanonicalText().equals(BIG_INTEGER)
                            && !type.getCanonicalText().equals(CHAR_SEQUENCE)
                            && !type.equals(PsiTypes.byteType())
                            && !type.equals(PsiTypes.shortType())
                            && !type.equals(PsiTypes.intType())
                            && !type.equals(PsiTypes.longType())) {
                        String source = getSource(isMethod, isField, annotationName, "AnnotationBigDecimal");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(), source,
                                DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case EMAIL, PATTERN, NOT_BLANK -> checkStringOnly(element, diagnostics, annotationName, isMethod, type, isField);
                case FUTURE, FUTURE_OR_PRESENT, PAST, PAST_OR_PRESENT -> {
                    String dataType = type.getCanonicalText();
                    String dataTypeFQName = getMatchedJavaElementName(classType, dataType,
                            SET_OF_DATE_TYPES.toArray(new String[0]));
                    if (dataTypeFQName == null) {
                        String source = getSource(isMethod, isField, annotationName, "AnnotationDate");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case MIN, MAX -> {
                    if (!type.getCanonicalText().equals(BIG_DECIMAL)
                            && !type.getCanonicalText().equals(BIG_INTEGER)
                            && !type.equals(PsiTypes.byteType())
                            && !type.equals(PsiTypes.shortType())
                            && !type.equals(PsiTypes.intType())
                            && !type.equals(PsiTypes.longType())) {
                        String source = getSource(isMethod, isField, annotationName, "AnnotationMinMax");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case NEGATIVE, NEGATIVE_OR_ZERO, POSITIVE, POSITIVE_OR_ZERO -> {
                    if (!type.getCanonicalText().equals(BIG_DECIMAL)
                            && !type.getCanonicalText().equals(BIG_INTEGER)
                            && !type.equals(PsiTypes.byteType())
                            && !type.equals(PsiTypes.shortType())
                            && !type.equals(PsiTypes.intType())
                            && !type.equals(PsiTypes.longType())
                            && !type.equals(PsiTypes.floatType())
                            && !type.equals(PsiTypes.doubleType())) {
                        String source = getSource(isMethod, isField, annotationName, "AnnotationPositive");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case NOT_EMPTY, SIZE -> {
                    if (!(isSizeOrNonEmptyAllowed(type))) {
                        String source = getSource(isMethod, isField, annotationName, "SizeOrNonEmptyAnnotations");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                    }
                }
                case VALID -> {
                    if (!isCascadableType(type)) {
                        String source = Messages.getMessage("InvalidValidAnnotation");
                        diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                                source, DIAGNOSTIC_CODE_INVALID_VALID_ANNOTATION, annotationName, DiagnosticSeverity.Error));
                    }
                }
                default -> LOGGER.log(Level.SEVERE, "Unexpected value for annotation");
            }
        }
    }

    /**
     * getSource message
     * @param isMethod
     * @param isField
     * @param annotationName
     * @param messageKey
     * @return
     */
    private static String getSource(boolean isMethod, boolean isField, String annotationName, String messageKey) {
        return isMethod ?
                Messages.getMessage(messageKey + "Methods", "@" + getSimpleName(annotationName)) : isField ?
                Messages.getMessage(messageKey + "Fields", "@" + getSimpleName(annotationName)) :
                Messages.getMessage(messageKey + "Params", "@" + getSimpleName(annotationName));

    }

    /**
     * isSizeOrNonEmptyAllowed
     * This method checks whether the supported types for the Size and NotEmpty annotations are CharSequence, Collection, Map, or array.
     *
     * @param childType
     * @return
     */
    public static boolean isSizeOrNonEmptyAllowed(PsiType childType) {

        if (childType instanceof PsiArrayType) {
            return true;
        }
        if (childType instanceof PsiPrimitiveType) {
            return false;
        }
        PsiClass resolvedClass = PsiUtil.resolveClassInClassTypeOnly(childType);
        return resolvedClass != null && (inheritsFrom(resolvedClass, CHAR_SEQUENCE)
                || inheritsFrom(resolvedClass, COLLECTION_FQ)
                || inheritsFrom(resolvedClass, MAP_FQ));
    }

    /**
     * isCascadableType
     * This method checks whether a type is cascadable for @Valid annotation.
     * Non-cascadable types include: primitives, primitive arrays, boxed types, String, and other simple types.
     * Cascadable types include: complex objects, object arrays, collections, and maps.
     *
     * @param type the type to check
     * @return true if the type is cascadable, false otherwise
     */
    public static boolean isCascadableType(PsiType type) {
        // Primitive types are not cascadable
        if (type instanceof PsiPrimitiveType) {
            return false;
        }

        // Check arrays: primitive arrays are NOT cascadable, object arrays are cascadable
        if (type instanceof PsiArrayType) {
            PsiType componentType = ((PsiArrayType) type).getComponentType();
            // If the component type is primitive, the array is not cascadable
            // Object arrays are cascadable
            return !(componentType instanceof PsiPrimitiveType);
        }

        // Get the canonical text for comparison
        String canonicalText = type.getCanonicalText();

        // Boxed primitive types are not cascadable
        if (WRAPPER_TYPES.contains(canonicalText)) {
            return false;
        }

        // Check against known non-cascadable types
        for (String nonCascadableType : NON_CASCADABLE_TYPES) {
            if (canonicalText.equals(nonCascadableType)) {
                return false;
            }
        }

        // Enum types are not cascadable
        PsiClass resolvedClass = PsiUtil.resolveClassInClassTypeOnly(type);
        if (resolvedClass != null && resolvedClass.isEnum()) {
            return false;
        }

        // Collections and Maps are cascadable
        if (resolvedClass != null && (inheritsFrom(resolvedClass, COLLECTION_FQ) ||
                                      inheritsFrom(resolvedClass, MAP_FQ))) {
            return true;
        }

        // All other complex types (custom classes, etc.) are cascadable
        return true;
    }

    private void checkStringOnly(PsiElement element, List<Diagnostic> diagnostics, String annotationName,
                                 boolean isMethod, PsiType type, boolean isField) {
        if (!type.getCanonicalText().equals(STRING)
                && !type.getCanonicalText().equals(CHAR_SEQUENCE)) {
            String source = getSource(isMethod, isField, annotationName, "AnnotationString");
            diagnostics.add(createDiagnostic(element, (PsiJavaFile) element.getContainingFile(),
                    source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
        }
    }

    /**
     * checkConflictingConstraints
     * Check for conflicting constraint annotations (e.g., @Min > @Max, @DecimalMin > @DecimalMax, @Size min > max).
     *
     * @param element     the PSI element (field, method, or parameter)
     * @param type        the declaring class
     * @param annotations the annotations on the element
     * @param diagnostics the list to add diagnostics to
     */
    private void checkConflictingConstraints(PsiJvmModifiersOwner element,
                                             PsiClass type,
                                             PsiAnnotation[] annotations,
                                             List<Diagnostic> diagnostics) {

        PsiAnnotation minAnnotation = null, maxAnnotation = null,
                decMinAnnotation = null, decMaxAnnotation = null,
                sizeAnnotation = null;

        for (PsiAnnotation annotation : annotations) {
            String matched = getMatchedJavaElementName(type, annotation.getQualifiedName(),
                    new String[]{MIN, MAX, DECIMAL_MIN, DECIMAL_MAX, SIZE});
            if (matched != null) {
                switch (matched) {
                    case MIN -> minAnnotation = annotation;
                    case MAX -> maxAnnotation = annotation;
                    case DECIMAL_MIN -> decMinAnnotation = annotation;
                    case DECIMAL_MAX -> decMaxAnnotation = annotation;
                    case SIZE -> sizeAnnotation = annotation;
                }
            }
        }

        // Build constraint checks
        List<ConstraintCheck> checks = new ArrayList<>();
        if (minAnnotation != null && maxAnnotation != null) {
            checks.add(new ConstraintCheck(minAnnotation, maxAnnotation,
                    "value", "value", Long::parseLong,
                    "ConflictingConstraintAnnotationsMinMax"));
        }
        if (decMinAnnotation != null && decMaxAnnotation != null) {
            checks.add(new ConstraintCheck(decMinAnnotation, decMaxAnnotation,
                    "value", "value", Double::parseDouble,
                    "ConflictingConstraintAnnotationsDecimalMinMax"));
        }
        if (sizeAnnotation != null) {
            checks.add(new ConstraintCheck(sizeAnnotation, sizeAnnotation,
                    "min", "max", Integer::parseInt,
                    "ConflictingConstraintAnnotationsSize"));
        }

        // Run all checks
        for (ConstraintCheck check : checks) {
            checkConflict(element, check, diagnostics);
        }
    }

    private void checkConflict(PsiJvmModifiersOwner element,
                               ConstraintCheck check,
                               List<Diagnostic> diagnostics) {

        var minStr = AnnotationUtils.getAnnotationMemberValue(check.minAnnotation(), check.minKey());
        var maxStr = AnnotationUtils.getAnnotationMemberValue(check.maxAnnotation(), check.maxKey());

        if (minStr != null && maxStr != null) {
            try {
                Number min = check.parser().apply(minStr);
                Number max = check.parser().apply(maxStr);
                if (min.doubleValue() > max.doubleValue()) {
                    diagnostics.add(createDiagnostic(
                            element,
                            (PsiJavaFile) element.getContainingFile(),
                            Messages.getMessage(check.messageKey(), minStr, maxStr),
                            DIAGNOSTIC_CODE_CONFLICTING_CONSTRAINTS,
                            null,
                            DiagnosticSeverity.Warning));
                }
            } catch (NumberFormatException e) {
                LOGGER.log(Level.INFO, () -> "Ignore invalid number format for " + check.messageKey());
            }
        }
    }

    private record ConstraintCheck(
            PsiAnnotation minAnnotation,
            PsiAnnotation maxAnnotation,
            String minKey,
            String maxKey,
            Function<String, Number> parser,
            String messageKey) {}

    /**
     * Quick check if a PsiType might contain TYPE_USE annotations.
     * Avoids expensive PSI traversal for simple types.
     *
     * @param type the PSI type to check
     * @return true if type might have TYPE_USE annotations
     */
    private boolean mightHaveTypeUseAnnotations(PsiType type) {
        if (type == null) {
            return false;
        }

        // Parameterized types (generics) might have TYPE_USE
        if (type instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) type;
            if (classType.getParameters().length > 0) {
                return true;
            }
        }

        // Array types might have TYPE_USE on component
        if (type instanceof PsiArrayType) {
            return true;
        }

        // Simple types cannot have TYPE_USE in their signature
        return false;
    }

    /**
     * Process TYPE_USE annotations on generic type arguments and array components.
     * Only called after lazy processing check confirms potential TYPE_USE annotations exist.
     *
     * @param element the PSI element (field, method, or parameter)
     * @param type the PSI type to process
     * @param typeElement the PSI element representing the type expression for diagnostic placement
     * @param classType the declaring class
     * @param diagnostics the list to add diagnostics to
     */
    private void processTypeUseAnnotations(PsiElement element, PsiType type, PsiElement typeElement, PsiClass classType, List<Diagnostic> diagnostics) {
        if (type == null) {
            return;
        }

        // Process parameterized types (generics)
        if (type instanceof PsiClassType) {
            PsiClassType psiClassType = (PsiClassType) type;
            if (psiClassType.getParameters().length > 0) {
                processGenericTypeArguments(element, psiClassType, typeElement, classType, diagnostics);
            }
        }

        // Process array types
        if (type instanceof PsiArrayType) {
            processArrayComponentType(element, (PsiArrayType) type, typeElement, classType, diagnostics);
        }
    }

    /**
     * Process annotations on generic type arguments.
     * Example: List<@Email String> - validates @Email on String
     *
     * @param element the PSI element
     * @param classType the parameterized type
     * @param typeElement the PSI element representing the type expression for diagnostic placement
     * @param declaringClass the declaring class
     * @param diagnostics the list to add diagnostics to
     */
    private void processGenericTypeArguments(PsiElement element, PsiClassType classType, PsiElement typeElement, PsiClass declaringClass, List<Diagnostic> diagnostics) {
        PsiType[] typeArguments = classType.getParameters();

        for (PsiType typeArg : typeArguments) {
            // Get annotations on this type argument
            PsiAnnotation[] annotations = typeArg.getAnnotations();

            for (PsiAnnotation annotation : annotations) {
                String matchedAnnotation = getMatchedJavaElementName(declaringClass,
                        annotation.getQualifiedName(),
                        SET_OF_ANNOTATIONS.toArray(new String[0]));

                if (matchedAnnotation != null) {
                    validateTypeUseAnnotation(element, typeElement, annotation, matchedAnnotation, typeArg, diagnostics, declaringClass);
                }
            }

            // Recursively process nested generics
            if (typeArg instanceof PsiClassType) {
                PsiClassType nestedClassType = (PsiClassType) typeArg;
                if (nestedClassType.getParameters().length > 0) {
                    processGenericTypeArguments(element, nestedClassType, typeElement, declaringClass, diagnostics);
                }
            }
        }
    }

    /**
     * Process annotations on array component types.
     * Example: @Email String[] - validates @Email on String (not String[])
     *
     * @param element the PSI element
     * @param arrayType the array type
     * @param typeElement the PSI element representing the type expression for diagnostic placement
     * @param declaringClass the declaring class
     * @param diagnostics the list to add diagnostics to
     */
    private void processArrayComponentType(PsiElement element, PsiArrayType arrayType, PsiElement typeElement, PsiClass declaringClass, List<Diagnostic> diagnostics) {
        PsiType componentType = arrayType.getComponentType();

        // Get annotations on the component type
        PsiAnnotation[] annotations = componentType.getAnnotations();

        for (PsiAnnotation annotation : annotations) {
            String matchedAnnotation = getMatchedJavaElementName(declaringClass,
                    annotation.getQualifiedName(),
                    SET_OF_ANNOTATIONS.toArray(new String[0]));

            if (matchedAnnotation != null) {
                validateTypeUseAnnotation(element, typeElement, annotation, matchedAnnotation, componentType, diagnostics, declaringClass);
            }
        }
    }

    /**
     * Validate a TYPE_USE annotation against the constrained type.
     *
     * @param element the PSI element for diagnostic location
     * @param typeElement the PSI element representing the type expression for diagnostic placement
     * @param annotation the annotation to validate
     * @param matchedAnnotation the matched constraint annotation name
     * @param constrainedType the actual type being constrained
     * @param diagnostics the list to add diagnostics to
     * @param classType the declaring class
     */
    private void validateTypeUseAnnotation(PsiElement element, PsiElement typeElement, PsiAnnotation annotation,
                                          String matchedAnnotation, PsiType constrainedType,
                                          List<Diagnostic> diagnostics, PsiClass classType) {
        String annotationName = annotation.getQualifiedName();

        // Unbox if needed
        PsiType type = constrainedType;
        if (type instanceof PsiClassType) {
            PsiType unboxed = PsiPrimitiveType.getUnboxedType(type);
            if (unboxed != null) {
                type = unboxed;
            }
        }

        // Validate based on constraint type (reuse existing validation logic)
        // For TYPE_USE annotations, create diagnostic on the type element (entire type expression)
        // This provides better context: Map<String, List<@Email Integer>> instead of just @Email
        switch (matchedAnnotation) {
            case ASSERT_FALSE, ASSERT_TRUE -> {
                if (!type.equals(PsiTypes.booleanType())) {
                    String source = Messages.getMessage("AnnotationBooleanMethods", "@" + getSimpleName(annotationName));
                    diagnostics.add(createDiagnostic(typeElement, (PsiJavaFile) element.getContainingFile(),
                            source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                }
            }
            case EMAIL, PATTERN, NOT_BLANK -> {
                if (!type.getCanonicalText().equals(STRING)
                        && !type.getCanonicalText().equals(CHAR_SEQUENCE)) {
                    String source = Messages.getMessage("AnnotationStringMethods", "@" + getSimpleName(annotationName));
                    diagnostics.add(createDiagnostic(typeElement, (PsiJavaFile) element.getContainingFile(),
                            source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                }
            }
            case DECIMAL_MAX, DECIMAL_MIN, DIGITS -> {
                if (!type.getCanonicalText().equals(BIG_DECIMAL)
                        && !type.getCanonicalText().equals(BIG_INTEGER)
                        && !type.getCanonicalText().equals(CHAR_SEQUENCE)
                        && !type.equals(PsiTypes.byteType())
                        && !type.equals(PsiTypes.shortType())
                        && !type.equals(PsiTypes.intType())
                        && !type.equals(PsiTypes.longType())) {
                    String source = Messages.getMessage("AnnotationBigDecimalMethods", "@" + getSimpleName(annotationName));
                    diagnostics.add(createDiagnostic(typeElement, (PsiJavaFile) element.getContainingFile(),
                            source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                }
            }
            case FUTURE, FUTURE_OR_PRESENT, PAST, PAST_OR_PRESENT -> {
                String dataType = type.getCanonicalText();
                String dataTypeFQName = getMatchedJavaElementName(classType, dataType,
                        SET_OF_DATE_TYPES.toArray(new String[0]));
                if (dataTypeFQName == null) {
                    String source = Messages.getMessage("AnnotationDateMethods", "@" + getSimpleName(annotationName));
                    diagnostics.add(createDiagnostic(typeElement, (PsiJavaFile) element.getContainingFile(),
                            source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                }
            }
            case MIN, MAX -> {
                if (!type.getCanonicalText().equals(BIG_DECIMAL)
                        && !type.getCanonicalText().equals(BIG_INTEGER)
                        && !type.equals(PsiTypes.byteType())
                        && !type.equals(PsiTypes.shortType())
                        && !type.equals(PsiTypes.intType())
                        && !type.equals(PsiTypes.longType())) {
                    String source = Messages.getMessage("AnnotationMinMaxMethods", "@" + getSimpleName(annotationName));
                    diagnostics.add(createDiagnostic(typeElement, (PsiJavaFile) element.getContainingFile(),
                            source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                }
            }
            case NEGATIVE, NEGATIVE_OR_ZERO, POSITIVE, POSITIVE_OR_ZERO -> {
                if (!type.getCanonicalText().equals(BIG_DECIMAL)
                        && !type.getCanonicalText().equals(BIG_INTEGER)
                        && !type.equals(PsiTypes.byteType())
                        && !type.equals(PsiTypes.shortType())
                        && !type.equals(PsiTypes.intType())
                        && !type.equals(PsiTypes.longType())
                        && !type.equals(PsiTypes.floatType())
                        && !type.equals(PsiTypes.doubleType())) {
                    String source = Messages.getMessage("AnnotationPositiveMethods", "@" + getSimpleName(annotationName));
                    diagnostics.add(createDiagnostic(typeElement, (PsiJavaFile) element.getContainingFile(),
                            source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                }
            }
            case NOT_EMPTY, SIZE -> {
                if (!(isSizeOrNonEmptyAllowed(type))) {
                    String source = Messages.getMessage("SizeOrNonEmptyAnnotationsMethods", "@" + getSimpleName(annotationName));
                    diagnostics.add(createDiagnostic(typeElement, (PsiJavaFile) element.getContainingFile(),
                            source, DIAGNOSTIC_CODE_INVALID_TYPE, annotationName, DiagnosticSeverity.Error));
                }
            }
            case VALID -> {
                if (!isCascadableType(type)) {
                    String source = Messages.getMessage("InvalidValidAnnotation");
                    diagnostics.add(createDiagnostic(typeElement, (PsiJavaFile) element.getContainingFile(),
                            source, DIAGNOSTIC_CODE_INVALID_VALID_ANNOTATION, annotationName, DiagnosticSeverity.Error));
                }
            }
            default -> LOGGER.log(Level.SEVERE, "Unexpected value for annotation");
        }
    }

}
