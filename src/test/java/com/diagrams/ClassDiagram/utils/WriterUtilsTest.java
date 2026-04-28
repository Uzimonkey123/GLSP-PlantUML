/*
 * File: WriterUtilTest.java
 * Author: Norman Babiak
 * Description: Simple tests for writer utils
 * Date: 28.4.2026
 */

package com.diagrams.ClassDiagram.utils;

import org.junit.jupiter.api.*;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WriterUtils Tests")
class WriterUtilsTest {

    @Test
    @DisplayName("isNullOrEmpty and isNotEmpty")
    void nullOrEmpty() {
        assertTrue(WriterUtils.isNullOrEmpty(null));
        assertTrue(WriterUtils.isNullOrEmpty(""));
        assertFalse(WriterUtils.isNullOrEmpty("x"));
        assertFalse(WriterUtils.isNotEmpty(null));
        assertTrue(WriterUtils.isNotEmpty("x"));
    }

    @Test
    @DisplayName("extractIndentation and applyIndentation")
    void indentation() {
        assertEquals("", WriterUtils.extractIndentation(null));
        assertEquals("    ", WriterUtils.extractIndentation("    class User"));
        assertEquals("\t\t", WriterUtils.extractIndentation("\t\tclass User"));
        assertEquals("    class User", WriterUtils.applyIndentation("class User", "    "));
    }

    @Test
    @DisplayName("visibilityToSymbol maps correctly, unknown returns 0")
    void visibility() {
        assertEquals('+', WriterUtils.visibilityToSymbol("public"));
        assertEquals('-', WriterUtils.visibilityToSymbol("private"));
        assertEquals('#', WriterUtils.visibilityToSymbol("protected"));
        assertEquals('~', WriterUtils.visibilityToSymbol("package_private"));
        assertEquals(0, WriterUtils.visibilityToSymbol(null));
        assertEquals(0, WriterUtils.visibilityToSymbol("unknown"));
    }

    @Test
    @DisplayName("replaceWordBoundary: replaces at boundaries, not within words")
    void replaceWordBoundary() {
        assertEquals("NewA --> B", WriterUtils.replaceWordBoundary("A --> B", "A", "NewA"));
        assertEquals("ClassA --> B", WriterUtils.replaceWordBoundary("ClassA --> B", "A", "X"));
        assertEquals("B --> B", WriterUtils.replaceWordBoundary("A --> A", "A", "B"));
        assertEquals("A_B --> C", WriterUtils.replaceWordBoundary("A_B --> C", "A", "X"));
    }

    @Test
    @DisplayName("quoteIfNeeded: quotes names with spaces or dashes")
    void quoteIfNeeded() {
        assertNull(WriterUtils.quoteIfNeeded(null));
        assertEquals("User", WriterUtils.quoteIfNeeded("User"));
        assertEquals("\"User Account\"", WriterUtils.quoteIfNeeded("User Account"));
        assertEquals("\"User-Account\"", WriterUtils.quoteIfNeeded("User-Account"));
        assertEquals("User_Account", WriterUtils.quoteIfNeeded("User_Account"));
    }

    @Test
    @DisplayName("parseRawMemberName strips visibility prefixes")
    void parseRawMemberName() {
        assertEquals("method()", WriterUtils.parseRawMemberName("+method()"));
        assertEquals("field", WriterUtils.parseRawMemberName("-field"));
        assertEquals("method()", WriterUtils.parseRawMemberName("#method()"));
        assertNull(WriterUtils.parseRawMemberName(null));
    }

    @Test
    @DisplayName("deriveMemberRef and formatMemberRef")
    void memberRef() {
        assertEquals("doSomething(String)", WriterUtils.deriveMemberRef("void doSomething(String)"));
        assertEquals("fieldName", WriterUtils.deriveMemberRef("fieldName : String"));
        assertNull(WriterUtils.deriveMemberRef(null));
        assertEquals("", WriterUtils.formatMemberRef(null));
        assertEquals("method", WriterUtils.formatMemberRef("method"));
        assertEquals("\"my method\"", WriterUtils.formatMemberRef("my method"));
    }

    @Test
    @DisplayName("replaceMemberRefsForToken: replaces quoted and unquoted refs")
    void replaceMemberRefs() {
        Map<String, String> refs = new HashMap<>();
        refs.put("oldMethod", "newMethod");
        assertEquals("User::newMethod", WriterUtils.replaceMemberRefsForToken("User::oldMethod", "User", refs));
        assertEquals("Order::oldMethod", WriterUtils.replaceMemberRefsForToken("Order::oldMethod", "User", refs));

        Map<String, String> quotedRefs = new HashMap<>();
        quotedRefs.put("old method", "new method");
        assertEquals("User::\"new method\"", WriterUtils.replaceMemberRefsForToken("User::\"old method\"", "User", quotedRefs));
    }
}
