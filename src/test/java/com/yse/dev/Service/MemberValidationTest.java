package com.yse.dev.Service;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MemberValidationTest {
    @Test void trimsAndAcceptsValidNames() {
        assertEquals("movie_123", MemberValidation.userId(" movie_123 "));
        assertEquals("영화산책", MemberValidation.nickname(" 영화산책 "));
    }
    @Test void rejectsBlankAndInvalidNames() {
        assertThrows(IllegalArgumentException.class, () -> MemberValidation.userId(null));
        assertThrows(IllegalArgumentException.class, () -> MemberValidation.userId("abc"));
        assertThrows(IllegalArgumentException.class, () -> MemberValidation.nickname("<script>"));
    }
    @Test void validatesCompositionAndBcryptByteLimit() {
        assertDoesNotThrow(() -> MemberValidation.password("Password9!"));
        assertDoesNotThrow(() -> MemberValidation.password("A1!" + "가".repeat(23)));
        assertThrows(IllegalArgumentException.class, () -> MemberValidation.password("A1!" + "가".repeat(24)));
        assertThrows(IllegalArgumentException.class, () -> MemberValidation.password("Password123"));
        assertThrows(IllegalArgumentException.class, () -> MemberValidation.password("Password1!\n"));
    }
}
