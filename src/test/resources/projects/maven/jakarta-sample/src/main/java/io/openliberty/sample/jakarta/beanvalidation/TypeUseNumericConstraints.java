package io.openliberty.sample.jakarta.beanvalidation;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Test class for TYPE_USE validation of numeric constraint annotations.
 * Tests @Min, @Max, @Positive, @Negative on generic type arguments.
 */
public class TypeUseNumericConstraints {

    // ========== VALID CASES ==========
    
    // Valid: @Min on Integer type argument
    private List<@Min(1) Integer> validMinList;
    
    // Valid: @Max on Long type argument
    private List<@Max(100) Long> validMaxList;
    
    // Valid: @Positive on Integer type argument
    private List<@Positive Integer> validPositiveList;
    
    // Valid: @Negative on Integer type argument
    private List<@Negative Integer> validNegativeList;
    
    // ========== INVALID CASES ==========
    
    // Invalid: @Min on String type argument (should be numeric)
    private List<@Min(1) String> invalidMinString;
    
    // Invalid: @Max on Boolean type argument (should be numeric)
    private List<@Max(100) Boolean> invalidMaxBoolean;
    
    // Invalid: @Positive on String type argument (should be numeric)
    private List<@Positive String> invalidPositiveString;
    
    // Invalid: @Negative on Boolean type argument (should be numeric)
    private List<@Negative Boolean> invalidNegativeBoolean;
}
