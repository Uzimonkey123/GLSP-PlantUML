package com.diagrams.ClassDiagram.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.HashMap;
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
        }

        @Test
        @DisplayName("should return true for empty string")
        void returnTrueForEmpty() {
            assertTrue(WriterUtils.isNullOrEmpty(""));
        }

        @Test
        @DisplayName("should return false for non-empty string")
        void returnFalseForNonEmpty() {
            assertFalse(WriterUtils.isNullOrEmpty("text"));
        }
    }

    @Nested
    @DisplayName("isNotEmpty")
    class IsNotEmptyTests {

        @Test
        @DisplayName("should return false for null")
        void returnFalseForNull() {
            assertFalse(WriterUtils.isNotEmpty(null));
        }

        @Test
        @DisplayName("should return false for empty string")
        void returnFalseForEmpty() {
            assertFalse(WriterUtils.isNotEmpty(""));
        }

        @Test
        @DisplayName("should return true for non-empty string")
        void returnTrueForNonEmpty() {
            assertTrue(WriterUtils.isNotEmpty("text"));
        }
    }

    @Nested
    @DisplayName("extractIndentation")
    class ExtractIndentationTests {

        @Test
        @DisplayName("should return empty for null")
        void returnEmptyForNull() {
            assertEquals("", WriterUtils.extractIndentation(null));
        }

        @Test
        @DisplayName("should extract spaces")
        void extractSpaces() {
            assertEquals("    ", WriterUtils.extractIndentation("    class User"));
        }

        @Test
        @DisplayName("should extract tabs")
        void extractTabs() {
            assertEquals("\t\t", WriterUtils.extractIndentation("\t\tclass User"));
        }

        @Test
        @DisplayName("should extract mixed whitespace")
        void extractMixed() {
            assertEquals("  \t ", WriterUtils.extractIndentation("  \t class User"));
        }

        @Test
        @DisplayName("should return empty for no indentation")
        void returnEmptyForNoIndent() {
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
        }

        @Test
        @DisplayName("should handle empty indent")
        void handleEmptyIndent() {
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
        }

        @Test
        @DisplayName("should return zero for no matches")
        void returnZeroForNoMatches() {
            assertEquals(0, WriterUtils.countChar("abc", 'x'));
        }

        @Test
        @DisplayName("should handle empty string")
        void handleEmptyString() {
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
        @DisplayName("should return zero for null")
        void returnZeroForNull() {
            assertEquals(0, WriterUtils.visibilityToSymbol(null));
        }

        @Test
        @DisplayName("should return zero for unknown visibility")
        void returnZeroForUnknown() {
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
        @DisplayName("should return null for null")
        void returnNullForNull() {
            assertNull(WriterUtils.quoteIfNeeded(null));
        }

        @Test
        @DisplayName("should not quote simple identifier")
        void notQuoteSimple() {
            assertEquals("User", WriterUtils.quoteIfNeeded("User"));
        }

        @Test
        @DisplayName("should quote string with spaces")
        void quoteWithSpaces() {
            assertEquals("\"User Account\"", WriterUtils.quoteIfNeeded("User Account"));
        }

        @Test
        @DisplayName("should quote string with special chars")
        void quoteWithSpecialChars() {
            assertEquals("\"User-Account\"", WriterUtils.quoteIfNeeded("User-Account"));
        }

        @Test
        @DisplayName("should not quote underscore")
        void notQuoteUnderscore() {
            assertEquals("User_Account", WriterUtils.quoteIfNeeded("User_Account"));
        }
    }

    @Nested
    @DisplayName("unquote")
    class UnquoteTests {

        @Test
        @DisplayName("should return null for null")
        void returnNullForNull() {
            assertNull(WriterUtils.unquote(null));
        }

        @Test
        @DisplayName("should unquote quoted string")
        void unquoteQuoted() {
            assertEquals("User Account", WriterUtils.unquote("\"User Account\""));
        }

        @Test
        @DisplayName("should return unquoted string as-is")
        void returnUnquotedAsIs() {
            assertEquals("User", WriterUtils.unquote("User"));
        }

        @Test
        @DisplayName("should handle empty quotes")
        void handleEmptyQuotes() {
            assertEquals("", WriterUtils.unquote("\"\""));
        }
    }

    @Nested
    @DisplayName("parseRawMemberName")
    class ParseRawMemberNameTests {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("should return null or empty as-is")
        void returnNullOrEmptyAsIs(String input) {
            assertEquals(input, WriterUtils.parseRawMemberName(input));
        }

        @Test
        @DisplayName("should strip public visibility")
        void stripPublicVisibility() {
            assertEquals("methodName()", WriterUtils.parseRawMemberName("+methodName()"));
        }

        @Test
        @DisplayName("should strip private visibility")
        void stripPrivateVisibility() {
            assertEquals("fieldName", WriterUtils.parseRawMemberName("-fieldName"));
        }

        @Test
        @DisplayName("should strip protected visibility")
        void stripProtectedVisibility() {
            assertEquals("method()", WriterUtils.parseRawMemberName("#method()"));
        }

        @Test
        @DisplayName("should strip package private visibility")
        void stripPackagePrivateVisibility() {
            assertEquals("method()", WriterUtils.parseRawMemberName("~method()"));
        }

        @Test
        @DisplayName("should strip backslash prefix")
        void stripBackslashPrefix() {
            assertEquals("escapedMethod()", WriterUtils.parseRawMemberName("\\escapedMethod()"));
        }

        @Test
        @DisplayName("should strip stereotypes except static/abstract/classifier")
        void stripStereotypes() {
            assertEquals("method()", WriterUtils.parseRawMemberName("{custom}method()"));
        }

        @Test
        @DisplayName("should preserve static stereotype")
        void preserveStaticStereotype() {
            String result = WriterUtils.parseRawMemberName("{static}method()");
            assertTrue(result.contains("{static}") || result.contains("method()"));
        }
    }

    @Nested
    @DisplayName("deriveMemberRef")
    class DeriveMemberRefTests {

        @Test
        @DisplayName("should extract method name with params")
        void extractMethodWithParams() {
            assertEquals("doSomething(String)", WriterUtils.deriveMemberRef("void doSomething(String)"));
        }

        @Test
        @DisplayName("should extract field name before colon")
        void extractFieldName() {
            assertEquals("fieldName", WriterUtils.deriveMemberRef("fieldName : String"));
        }

        @Test
        @DisplayName("should return simple name as-is")
        void returnSimpleAsIs() {
            assertEquals("simpleName", WriterUtils.deriveMemberRef("simpleName"));
        }

        @Test
        @DisplayName("should handle null")
        void handleNull() {
            assertNull(WriterUtils.deriveMemberRef(null));
        }

        @Test
        @DisplayName("should handle empty")
        void handleEmpty() {
            assertEquals("", WriterUtils.deriveMemberRef(""));
        }
    }

    @Nested
    @DisplayName("formatMemberRef")
    class FormatMemberRefTests {

        @Test
        @DisplayName("should return empty for null")
        void returnEmptyForNull() {
            assertEquals("", WriterUtils.formatMemberRef(null));
        }

        @Test
        @DisplayName("should return simple ref unquoted")
        void returnSimpleUnquoted() {
            assertEquals("method", WriterUtils.formatMemberRef("method"));
        }

        @Test
        @DisplayName("should quote ref with space")
        void quoteWithSpace() {
            assertEquals("\"my method\"", WriterUtils.formatMemberRef("my method"));
        }

        @Test
        @DisplayName("should quote ref with parentheses")
        void quoteWithParens() {
            assertEquals("\"method()\"", WriterUtils.formatMemberRef("method()"));
        }

        @Test
        @DisplayName("should quote ref with colon")
        void quoteWithColon() {
            assertEquals("\"field: String\"", WriterUtils.formatMemberRef("field: String"));
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
        @DisplayName("should replace reference with new token")
        void replaceWithNewToken() {
            String result = WriterUtils.replaceReference("User --> Order", "User", "Person", false);
            assertEquals("Person --> Order", result);
        }

        @Test
        @DisplayName("should replace quoted reference")
        void replaceQuotedReference() {
            String result = WriterUtils.replaceReference("\"User\" --> Order", "User", "Person", false);
            assertEquals("\"Person\" --> Order", result);
        }

        @Test
        @DisplayName("should not replace when same name")
        void notReplaceWhenSame() {
            String result = WriterUtils.replaceReference("User --> Order", "User", "User", false);
            assertEquals("User --> Order", result);
        }

        @Test
        @DisplayName("should not replace partial match")
        void notReplacePartialMatch() {
            String result = WriterUtils.replaceReference("UserAccount --> Order", "User", "Person", false);
            assertEquals("UserAccount --> Order", result);
        }

        @Test
        @DisplayName("strict mode should not match after dot")
        void strictModeNoDotMatch() {
            String result = WriterUtils.replaceReference("com.User --> Order", "User", "Person", true);
            assertEquals("com.User --> Order", result);
        }

        @Test
        @DisplayName("non-strict mode should match after dot")
        void nonStrictModeDotMatch() {
            String result = WriterUtils.replaceReference("com.User --> Order", "User", "Person", false);
            assertEquals("com.Person --> Order", result);
        }
    }
}