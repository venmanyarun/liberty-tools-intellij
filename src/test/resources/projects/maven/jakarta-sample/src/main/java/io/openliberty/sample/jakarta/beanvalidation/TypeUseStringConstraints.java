package io.openliberty.sample.jakarta.beanvalidation;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Test class for TYPE_USE validation of String constraint annotations.
 * Tests @Email, @NotBlank, @Pattern on generic type arguments.
 */
public class TypeUseStringConstraints {

    // ========== VALID CASES ==========
    
    // Valid: @Email on String type argument
    private List<@Email String> validEmailList;
    
    // Valid: @NotBlank on String type argument
    private List<@NotBlank String> validNotBlankList;
    
    // Valid: @Pattern on String type argument
    private List<@Pattern(regexp = ".*") String> validPatternList;
    
    // ========== INVALID CASES ==========
    
    // Invalid: @Email on Integer type argument (should be String/CharSequence)
    private List<@Email Integer> invalidEmailInteger;
    
    // Invalid: @NotBlank on Integer type argument (should be String/CharSequence)
    private List<@NotBlank Integer> invalidNotBlankInteger;
    
    // Invalid: @Pattern on Boolean type argument (should be String/CharSequence)
    private List<@Pattern(regexp = ".*") Boolean> invalidPatternBoolean;
}
