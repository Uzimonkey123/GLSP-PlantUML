package com.diagrams.ClassDiagram.parser;

import com.diagrams.ClassDiagram.model.ClassParts.ClassEntity;
import com.diagrams.ClassDiagram.model.ClassParts.EntityMethod;
import net.sourceforge.plantuml.abel.Entity;
import net.sourceforge.plantuml.klimt.color.ColorType;
import net.sourceforge.plantuml.klimt.color.Colors;
import net.sourceforge.plantuml.klimt.color.HColor;
import net.sourceforge.plantuml.klimt.creole.Display;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
    @Mock private HColor mockBackColor;

    private ClassEntity targetEntity;

    @BeforeEach
    void setup() {
        tipsHandler = new TipsHandler();
        targetEntity = new ClassEntity(0, 0, "test-entity", "TestClass", "CLASS");
    }

    private void setupTips(Map<String, Display> tips) {
        when(mockTipsEntity.getTips()).thenReturn(tips);
        when(mockTipsEntity.getColors()).thenReturn(mockColors);
        when(mockColors.getColor(ColorType.BACK)).thenReturn(null);
    }

    private Map<String, Display> singleTip(String key, String... lines) {
        Map<String, Display> tips = new HashMap<>();
        tips.put(key, Display.create(lines));

        return tips;
    }

    @Nested
    @DisplayName("Tip Application")
    class TipApplicationTests {

        @Test
        @DisplayName("applies tip to matching method")
        void applyTipToMethod() {
            EntityMethod method = new EntityMethod("doSomething()");
            targetEntity.getMethods().add(method);
            setupTips(singleTip("doSomething", "Method tip"));

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertEquals("Method tip", method.getTip());
        }

        @Test
        @DisplayName("applies tip to raw body member")
        void applyTipToRawBody() {
            EntityMethod member = new EntityMethod("customMethod()");
            targetEntity.getRawBody().add(member);
            setupTips(singleTip("customMethod", "Raw body tip"));

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertEquals("Raw body tip", member.getTip());
        }

        @Test
        @DisplayName("applies background color from tips entity")
        void applyBackgroundColor() {
            EntityMethod method = new EntityMethod("test()");
            targetEntity.getMethods().add(method);

            when(mockTipsEntity.getTips()).thenReturn(singleTip("test", "Tip"));
            when(mockTipsEntity.getColors()).thenReturn(mockColors);
            when(mockColors.getColor(ColorType.BACK)).thenReturn(mockBackColor);
            when(mockBackColor.asString()).thenReturn("#FFFF00");

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertEquals("#FFFF00", method.getTipBackground());
        }

        @Test
        @DisplayName("joins multiline tips with <br>")
        void handleMultilineTip() {
            EntityMethod method = new EntityMethod("multi()");
            targetEntity.getMethods().add(method);
            setupTips(singleTip("multi", "Line 1", "Line 2", "Line 3"));

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertEquals("Line 1<br>Line 2<br>Line 3", method.getTip());
        }

        @Test
        @DisplayName("does not apply tip to non-matching member")
        void noTipForNonMatching() {
            EntityMethod method = new EntityMethod("differentMethod()");
            targetEntity.getMethods().add(method);
            setupTips(singleTip("someOtherMethod", "Tip"));

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertNull(method.getTip());
        }
    }

    @Nested
    @DisplayName("Signature Matching")
    class SignatureMatchingTests {

        @Test
        @DisplayName("matches method stripping visibility prefix (+/-/#/~)")
        void matchWithVisibilityPrefix() {
            EntityMethod method = new EntityMethod("+publicMethod()");
            targetEntity.getMethods().add(method);
            setupTips(singleTip("publicMethod", "Tip"));

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertEquals("Tip", method.getTip());
        }

        @Test
        @DisplayName("matches method stripping {static} modifier")
        void matchWithStaticModifier() {
            EntityMethod method = new EntityMethod("{static} getInstance()");
            targetEntity.getMethods().add(method);
            setupTips(singleTip("getInstance", "Static tip"));

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertEquals("Static tip", method.getTip());
        }

        @Test
        @DisplayName("matches method with full signature including parameters")
        void matchWithFullSignature() {
            EntityMethod method = new EntityMethod("process(String)");
            targetEntity.getMethods().add(method);
            setupTips(singleTip("process(String)", "Processing tip"));

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertEquals("Processing tip", method.getTip());
        }

        @Test
        @DisplayName("matches method by name only")
        void matchByNameOnly() {
            EntityMethod method = new EntityMethod("calculate(int, int)");
            targetEntity.getMethods().add(method);
            setupTips(singleTip("calculate", "Calc tip"));

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertEquals("Calc tip", method.getTip());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("handles empty tips map without error")
        void handleEmptyTipsMap() {
            targetEntity.getMethods().add(new EntityMethod("method()"));
            when(mockTipsEntity.getTips()).thenReturn(new HashMap<>());

            assertDoesNotThrow(() -> tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity));
        }

        @Test
        @DisplayName("handles entity with no members")
        void handleEmptyMemberLists() {
            setupTips(singleTip("anyMember", "Tip"));

            assertDoesNotThrow(() -> tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity));
        }

        @Test
        @DisplayName("handles null target entity gracefully")
        void handleNullTargetEntity() {
            setupTips(singleTip("method", "Tip"));

            assertDoesNotThrow(() -> tipsHandler.applyTipsToEntity(mockTipsEntity, null));
        }

        @Test
        @DisplayName("applies tips to multiple members")
        void applyTipsToMultipleMembers() {
            EntityMethod method1 = new EntityMethod("first()");
            EntityMethod method2 = new EntityMethod("second()");
            targetEntity.getMethods().add(method1);
            targetEntity.getMethods().add(method2);

            Map<String, Display> tips = new HashMap<>();
            tips.put("first", Display.create("First tip"));
            tips.put("second", Display.create("Second tip"));
            setupTips(tips);

            tipsHandler.applyTipsToEntity(mockTipsEntity, targetEntity);

            assertAll(
                    () -> assertEquals("First tip", method1.getTip()),
                    () -> assertEquals("Second tip", method2.getTip())
            );
        }
    }
}