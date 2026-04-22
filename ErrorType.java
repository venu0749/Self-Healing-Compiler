/**
 * ErrorType — All 10 error types the compiler can detect and heal
 */
public enum ErrorType {
    MISSING_SEMICOLON,
    UNDECLARED_VARIABLE,
    DUPLICATE_DECLARATION,
    TYPE_MISMATCH,
    PRINTF_MISMATCH,
    ASSIGNMENT_ERROR,
    MISSING_RETURN,
    UNSAFE_FUNCTION,
    MISSING_ADDRESS_OP,
    ARRAY_WITHOUT_SIZE,
    UNKNOWN
}
 