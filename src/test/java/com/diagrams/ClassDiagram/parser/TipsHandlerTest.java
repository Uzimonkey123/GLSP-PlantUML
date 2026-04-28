/*
 * File: TipsHandlerTest.java
 * Author: Norman Babiak
 * Description: Tests for entity method tips
 * Date: 28.4.2026
 */

package com.diagrams.ClassDiagram.parser;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import net.sourceforge.plantuml.abel.Entity;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.klimt.color.Colors;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.creole.Display;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TipsHandler Tests")
class TipsHandlerTest {
    private TipsHandler tipsHandler;
    @Mock private Entity mockTipsEntity;
    @Mock private Colors mockColors;
    private ClassEntity target;

    @BeforeEach
    void setup() {
        tipsHandler = new TipsHandler();
        target = new ClassEntity(0, 0, "e", "E", "CLASS");
        when(mockTipsEntity.getColors()).thenReturn(mockColors);
        when(mockColors.getColor(ColorType.BACK)).thenReturn(null);
    }

    private void tips(Map<String, Display> tips) {
        when(mockTipsEntity.getTips()).thenReturn(tips);
    }

    @Test
    @DisplayName("applies tip to matching method")
    void applyTipAndMultiline() {
        EntityMethod method = new EntityMethod("doWork()");
        target.getMethods().add(method);
        Map<String, Display> tipsMap = new HashMap<>();
        tipsMap.put("doWork", Display.create("Line 1", "Line 2"));
        tips(tipsMap);

        tipsHandler.applyTipsToEntity(mockTipsEntity, target);

        assertEquals("Line 1<br>Line 2", method.getTip());
    }

    @Test
    @DisplayName("applies background color from tips entity")
    void applyBackground() {
        EntityMethod method = new EntityMethod("test()");
        target.getMethods().add(method);
        tips(Map.of("test", Display.create("Tip")));
        HColor mockColor = mock(HColor.class);
        when(mockColors.getColor(ColorType.BACK)).thenReturn(mockColor);
        when(mockColor.asString()).thenReturn("#FFFF00");

        tipsHandler.applyTipsToEntity(mockTipsEntity, target);

        assertEquals("#FFFF00", method.getTipBackground());
    }

    @Test
    @DisplayName("strips visibility prefix when matching")
    void visibilityStripping() {
        EntityMethod method = new EntityMethod("+publicMethod()");
        target.getMethods().add(method);
        tips(Map.of("publicMethod", Display.create("Tip")));

        tipsHandler.applyTipsToEntity(mockTipsEntity, target);

        assertEquals("Tip", method.getTip());
    }

    @Test
    @DisplayName("does not apply tip to non-matching member")
    void noMatch() {
        EntityMethod method = new EntityMethod("other()");
        target.getMethods().add(method);
        tips(Map.of("different", Display.create("Tip")));

        tipsHandler.applyTipsToEntity(mockTipsEntity, target);

        assertNull(method.getTip());
    }

    @Test
    @DisplayName("handles empty tips, null target, and multiple members")
    void edgeCases() {
        when(mockTipsEntity.getTips()).thenReturn(new HashMap<>());
        assertDoesNotThrow(() -> tipsHandler.applyTipsToEntity(mockTipsEntity, target));
        assertDoesNotThrow(() -> tipsHandler.applyTipsToEntity(mockTipsEntity, null));

        EntityMethod method1 = new EntityMethod("a()");
        EntityMethod method2 = new EntityMethod("b()");
        target.getMethods().add(method1);
        target.getMethods().add(method2);
        Map<String, Display> tipsMap = new HashMap<>();
        tipsMap.put("a", Display.create("T1"));
        tipsMap.put("b", Display.create("T2"));
        tips(tipsMap);

        tipsHandler.applyTipsToEntity(mockTipsEntity, target);

        assertEquals("T1", method1.getTip());
        assertEquals("T2", method2.getTip());
    }
}
