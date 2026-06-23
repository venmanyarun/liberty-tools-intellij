package io.openliberty.sample.jakarta.beanvalidation;

import jakarta.validation.constraints.*;

/**
 * Test class for TYPE_USE validation of constraints on array component types.
 * Tests @Email, @NotBlank, @AssertTrue on array components.
 */
public class TypeUseArrayConstraints {

    // ========== VALID CASES ==========
    
    // Valid: @Email on String array component
    private @Email String[] validEmailArray;
    
    // Valid: @NotBlank on String array component
    private @NotBlank String[] validNotBlankArray;
    
    // Valid: @AssertTrue on Boolean array component
    private @AssertTrue Boolean[] validAssertTrueArray;
    
    // ========== INVALID CASES ==========
    
    // Invalid: @Email on Integer array component
    private @Email Integer[] invalidEmailIntegerArray;
    
    // Invalid: @NotBlank on Boolean array component
    private @NotBlank Boolean[] invalidNotBlankBooleanArray;
    
    // Invalid: @AssertTrue on String array component
    private @AssertTrue String[] invalidAssertTrueStringArray;
}
