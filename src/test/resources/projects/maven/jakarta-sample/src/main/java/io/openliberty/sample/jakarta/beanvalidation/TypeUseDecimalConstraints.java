package io.openliberty.sample.jakarta.beanvalidation;

import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Map;

/**
 * Test class for TYPE_USE validation of decimal constraint annotations.
 * Tests @DecimalMin, @DecimalMax, @Digits on generic type arguments and nested generics.
 */
public class TypeUseDecimalConstraints {

    // ========== VALID CASES ==========
    
    // Valid: @DecimalMin on Integer type argument
    private List<@DecimalMin("0.0") Integer> validDecimalMinList;
    
    // Valid: @DecimalMax on Long type argument
    private List<@DecimalMax("100.0") Long> validDecimalMaxList;
    
    // Valid: @Digits on Integer type argument
    private List<@Digits(integer = 3, fraction = 2) Integer> validDigitsList;
    
    // Valid: Nested generics with @Email on String
    private Map<String, List<@Email String>> validNestedEmailMap;
    
    // ========== INVALID CASES ==========
    
    // Invalid: Nested generics with @Email on Integer
    private Map<String, List<@Email Integer>> invalidNestedEmailMap;
    
    // Invalid: @DecimalMin on Boolean type argument
    private List<@DecimalMin("0.0") Boolean> invalidDecimalMinBoolean;
    
    // Invalid: @DecimalMax on Boolean type argument
    private List<@DecimalMax("100.0") Boolean> invalidDecimalMaxBoolean;
    
    // Invalid: @Digits on Boolean type argument
    private List<@Digits(integer = 3, fraction = 2) Boolean> invalidDigitsBoolean;
}
