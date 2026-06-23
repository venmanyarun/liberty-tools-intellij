package io.openliberty.sample.jakarta.beanvalidation;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

/**
 * Test class for TYPE_USE validation of constraints on complex nested generic types.
 * Tests multiple annotations on complex nested structures.
 */
public class TypeUseComplexConstraints {

    // ========== VALID COMPLEX NESTED GENERICS ==========
    
    // Valid: @Email on String in nested Map
    private Map<String, List<@Email String>> validNestedEmails;
    
    // Valid: @NotBlank on String in nested structure
    private Map<@NotBlank String, List<String>> validNestedKeys;
    
    // ========== INVALID COMPLEX NESTED GENERICS ==========
    
    // Invalid: @Email on Integer in nested Map value
    private Map<String, List<@Email Integer>> invalidNestedEmails;
    
    // Invalid: @NotBlank on Integer in nested Map key
    private Map<@NotBlank Integer, List<String>> invalidNestedKeys;
}
