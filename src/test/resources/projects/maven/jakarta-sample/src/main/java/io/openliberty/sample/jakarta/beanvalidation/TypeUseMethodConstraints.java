package io.openliberty.sample.jakarta.beanvalidation;

import jakarta.validation.constraints.*;
import java.util.List;

/**
 * Test class for TYPE_USE validation of constraints on method return types and parameters.
 * Tests @Email on method return types and parameters.
 */
public class TypeUseMethodConstraints {

    // ========== VALID METHOD RETURN TYPES ==========
    
    // Valid: @Email on String in return type
    public List<@Email String> getValidEmails() {
        return null;
    }
    
    // Valid: @Email on String array return type
    public @Email String[] getValidEmailArray() {
        return null;
    }
    
    // ========== INVALID METHOD RETURN TYPES ==========
    
    // Invalid: @Email on Integer in return type
    public List<@Email Integer> getInvalidEmails() {
        return null;
    }
    
    // Invalid: @Email on Integer array return type
    public @Email Integer[] getInvalidEmailArray() {
        return null;
    }
    
    // ========== VALID METHOD PARAMETERS ==========
    
    // Valid: @Email on String in parameter type
    public void setValidEmails(List<@Email String> emails) {
    }
    
    // Valid: @Email on String array parameter
    public void setValidEmailArray(@Email String[] emails) {
    }
    
    // ========== INVALID METHOD PARAMETERS ==========
    
    // Invalid: @Email on Integer in parameter type
    public void setInvalidEmails(List<@Email Integer> numbers) {
    }
    
    // Invalid: @Email on Integer array parameter
    public void setInvalidEmailArray(@Email Integer[] numbers) {
    }
}
