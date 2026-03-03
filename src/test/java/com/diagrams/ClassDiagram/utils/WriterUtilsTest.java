package com.diagrams.ClassDiagram.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WriterUtils Tests")
class WriterUtilsTest {

    @Nested
    @DisplayName("isNullOrEmpty")
    class IsNullOrEmptyTests {

        @Test
        @DisplayName("should return true for null")
        void returnTrueForNull() {
            assertTrue(WriterUtils.isNullOrEmpty(null));
            assertTrue(WriterUtils.isNullOrEmpty(""));
            assertFalse(WriterUtils.isNullOrEmpty("text"));
        }
    }

    @Nested
    @DisplayName("isNotEmpty")
    class IsNotEmptyTests {

        @Test
        @DisplayName("should return false for null and empty")
        void returnFalseForNull() {
            assertFalse(WriterUtils.isNotEmpty(null));
            assertFalse(WriterUtils.isNotEmpty(""));
            assertTrue(WriterUtils.isNotEmpty("text"));
        }
    }

    @Nested
    @DisplayName("extractIndentation")
    class ExtractIndentationTests {

        @Test
        @DisplayName("should extract spaces")
        void extractSpaces() {
            assertEquals("", WriterUtils.extractIndentation(null));
            assertEquals("    ", WriterUtils.extractIndentation("    class User"));
            assertEquals("\t\t", WriterUtils.extractIndentation("\t\tclass User"));
            assertEquals("  \t ", WriterUtils.extractIndentation("  \t class User"));
            assertEquals("", WriterUtils.extractIndentation("class User"));
        }
    }

    @Nested
    @DisplayName("applyIndentation")
    class ApplyIndentationTests {

        @Test
        @DisplayName("should prepend indent")
        void prependIndent() {
            assertEquals("    class User", WriterUtils.applyIndentation("class User", "    "));
            assertEquals("class User", WriterUtils.applyIndentation("class User", ""));
        }
    }

    @Nested
    @DisplayName("countChar")
    class CountCharTests {

        @Test
        @DisplayName("should count occurrences")
        void countOccurrences() {
            assertEquals(3, WriterUtils.countChar("ababa", 'a'));
            assertEquals(0, WriterUtils.countChar("abc", 'x'));
            assertEquals(0, WriterUtils.countChar("", 'a'));
        }
    }

    @Nested
    @DisplayName("visibilityToSymbol")
    class VisibilityToSymbolTests {

        @ParameterizedTest
        @CsvSource({
                "private, -",
                "protected, #",
                "package_private, ~",
                "public, +"
        })
        @DisplayName("should convert visibility to symbol")
        void convertVisibility(String visibility, char expected) {
            assertEquals(expected, WriterUtils.visibilityToSymbol(visibility));
        }

        @Test
        @DisplayName("should return zero")
        void returnZero() {
            assertEquals(0, WriterUtils.visibilityToSymbol(null));
            assertEquals(0, WriterUtils.visibilityToSymbol("unknown"));
        }
    }

    @Nested
    @DisplayName("replaceWordBoundary")
    class ReplaceWordBoundaryTests {

        @Test
        @DisplayName("should replace at word boundary")
        void replaceAtBoundary() {
            String result = WriterUtils.replaceWordBoundary("A --> B", "A", "NewA");
            assertEquals("NewA --> B", result);
        }

        @Test
        @DisplayName("should not replace within word")
        void notReplaceWithinWord() {
            String result = WriterUtils.replaceWordBoundary("ClassA --> B", "A", "X");
            assertEquals("ClassA --> B", result);

            result = WriterUtils.replaceWordBoundary("List --> AbstractList", "List", "Test");
            assertEquals("Test --> AbstractList", result);
        }

        @Test
        @DisplayName("should replace multiple occurrences")
        void replaceMultiple() {
            String result = WriterUtils.replaceWordBoundary("A --> A", "A", "B");
            assertEquals("B --> B", result);
        }

        @Test
        @DisplayName("should handle underscore boundary")
        void handleUnderscoreBoundary() {
            String result = WriterUtils.replaceWordBoundary("A_B --> C", "A", "X");
            assertEquals("A_B --> C", result);
        }

        @Test
        @DisplayName("should replace at start of string")
        void replaceAtStart() {
            String result = WriterUtils.replaceWordBoundary("User --> Order", "User", "Person");
            assertEquals("Person --> Order", result);
        }

        @Test
        @DisplayName("should replace at end of string")
        void replaceAtEnd() {
            String result = WriterUtils.replaceWordBoundary("User --> Order", "Order", "Purchase");
            assertEquals("User --> Purchase", result);
        }
    }

    @Nested
    @DisplayName("quoteIfNeeded")
    class QuoteIfNeededTests {

        @Test
        @DisplayName("should decide if quote is needed or not")
        void returnQuotes() {
            assertNull(WriterUtils.quoteIfNeeded(null));
            assertEquals("User", WriterUtils.quoteIfNeeded("User"));
            assertEquals("\"User Account\"", WriterUtils.quoteIfNeeded("User Account"));
            assertEquals("\"User-Account\"", WriterUtils.quoteIfNeeded("User-Account"));
            assertEquals("User_Account", WriterUtils.quoteIfNeeded("User_Account"));
        }
    }

    @Nested
    @DisplayName("parseRawMemberName")
    class ParseRawMemberNameTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return stripped or preserved member names")
        void returnPreservedOrStripped(String input) {
            assertEquals(input, WriterUtils.parseRawMemberName(input));
            assertEquals("methodName()", WriterUtils.parseRawMemberName("+methodName()"));
            assertEquals("fieldName", WriterUtils.parseRawMemberName("-fieldName"));
            assertEquals("method()", WriterUtils.parseRawMemberName("#method()"));
            assertEquals("method()", WriterUtils.parseRawMemberName("~method()"));
            assertEquals("escapedMethod()", WriterUtils.parseRawMemberName("\\escapedMethod()"));
            assertEquals("method()", WriterUtils.parseRawMemberName("{custom}method()"));

            String result = WriterUtils.parseRawMemberName("{static}method()");
            assertTrue(result.contains("{static}") || result.contains("method()"));
        }
    }

    @Nested
    @DisplayName("deriveMemberRef")
    class DeriveMemberRefTests {

        @Test
        @DisplayName("should handle all deriveMemberRef scenarios")
        void deriveMemberRef_allScenarios() {
            assertEquals("doSomething(String)", WriterUtils.deriveMemberRef("void doSomething(String)"),
                    "Failed for method with params");

            assertEquals("fieldName", WriterUtils.deriveMemberRef("fieldName : String"),
                    "Failed for field name with colon");

            assertEquals("simpleName", WriterUtils.deriveMemberRef("simpleName"),
                    "Failed for simple name");

            assertNull(WriterUtils.deriveMemberRef(null),
                    "Failed for null input");

            assertEquals("", WriterUtils.deriveMemberRef(""),
                    "Failed for empty input");
        }
    }

    @Nested
    @DisplayName("formatMemberRef")
    class FormatMemberRefTests {

        @Test
        @DisplayName("should handle all formatMemberRef scenarios")
        void formatMemberRef_allScenarios() {
            assertEquals("", WriterUtils.formatMemberRef(null), "Failed for null input");
            assertEquals("method", WriterUtils.formatMemberRef("method"), "Failed for simple name");
            assertEquals("\"my method\"", WriterUtils.formatMemberRef("my method"), "Failed for name with space");
            assertEquals("\"method()\"", WriterUtils.formatMemberRef("method()"), "Failed for name with parentheses");
            assertEquals("\"field: String\"", WriterUtils.formatMemberRef("field: String"), "Failed for name with colon");
        }
    }

    @Nested
    @DisplayName("replaceMemberRefsForToken")
    class ReplaceMemberRefsForTokenTests {

        @Test
        @DisplayName("should replace unquoted ref")
        void replaceUnquotedRef() {
            Map<String, String> refMap = new HashMap<>();
            refMap.put("oldMethod", "newMethod");

            String result = WriterUtils.replaceMemberRefsForToken("User::oldMethod", "User", refMap);
            assertEquals("User::newMethod", result);
        }

        @Test
        @DisplayName("should replace quoted ref")
        void replaceQuotedRef() {
            Map<String, String> refMap = new HashMap<>();
            refMap.put("old method", "new method");

            String result = WriterUtils.replaceMemberRefsForToken("User::\"old method\"", "User", refMap);
            assertEquals("User::\"new method\"", result);
        }

        @Test
        @DisplayName("should not replace non-matching token")
        void notReplaceNonMatchingToken() {
            Map<String, String> refMap = new HashMap<>();
            refMap.put("method", "newMethod");

            String result = WriterUtils.replaceMemberRefsForToken("Order::method", "User", refMap);
            assertEquals("Order::method", result);
        }
    }

    @Nested
    @DisplayName("replaceReference")
    class ReplaceReferenceTests {

        @Test
        @DisplayName("should handle all replaceReference scenarios")
        void replaceReference_allScenarios() {
            record Case(String input, String oldName, String newName, boolean strict, String expected) {}

            List<Case> cases = List.of(
                    new Case("User --> Order", "User", "Person", false,
                            "Person --> Order"),

                    new Case("\"User\" --> Order", "User", "Person", false,
                            "\"Person\" --> Order"),

                    new Case("User --> Order", "User", "User", false,
                            "User --> Order"),

                    new Case("UserAccount --> Order", "User", "Person", false,
                            "UserAccount --> Order"),

                    new Case("com.User --> Order", "User", "Person", true,
                            "com.User --> Order"),

                    new Case("com.User --> Order", "User", "Person", false,
                            "com.Person --> Order")
            );

            for (Case c : cases) {
                String result = WriterUtils.replaceReference(
                        c.input(),
                        c.oldName(),
                        c.newName(),
                        c.strict()
                );

                assertEquals(c.expected(), result, () -> "Failed for input: " + c.input() + ", strict=" + c.strict());
            }
        }
    }
}