package io.openliberty.sample.jakarta.beanvalidation;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Test class for TYPE_USE validation of Boolean constraint annotations.
 * Tests @AssertTrue, @AssertFalse on generic type arguments.
 */
public class TypeUseBooleanConstraints {

    // ========== VALID CASES ==========
    
    // Valid: @AssertTrue on Boolean type argument
    private List<@AssertTrue Boolean> validAssertTrueList;
    
    // Valid: @AssertFalse on Boolean type argument
    private List<@AssertFalse Boolean> validAssertFalseList;
    
    // ========== INVALID CASES ==========
    
    // Invalid: @AssertTrue on String type argument (should be boolean/Boolean)
    private List<@AssertTrue String> invalidAssertTrueString;
    
    // Invalid: @AssertFalse on Integer type argument (should be boolean/Boolean)
    private List<@AssertFalse Integer> invalidAssertFalseInteger;
}
