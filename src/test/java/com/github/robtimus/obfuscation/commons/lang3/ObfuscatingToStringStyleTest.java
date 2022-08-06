/*
 * ObfuscatingToStringStyleTest.java
 * Copyright 2020 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.obfuscation.commons.lang3;

import static com.github.robtimus.obfuscation.Obfuscator.fixedLength;
import static com.github.robtimus.obfuscation.Obfuscator.none;
import static com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.defaultStyle;
import static com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.multiLineRecursiveStyle;
import static com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.recursiveStyle;
import static com.github.robtimus.obfuscation.support.CaseSensitivity.CASE_INSENSITIVE;
import static com.github.robtimus.obfuscation.support.CaseSensitivity.CASE_SENSITIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.Builder;

@SuppressWarnings("nls")
class ObfuscatingToStringStyleTest {

    @Nested
    @DisplayName("defaultStyle()")
    class DefaultStyle extends ToStringStyleTest {

        DefaultStyle() {
            super(ObfuscatingToStringStyle::defaultStyle,
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForDefaultStyle,
                    ObfuscatingToStringStyleTest::expectedArrayReflectionToStringForDefaultStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithObfuscateSummariesForDefaultStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithoutObfuscateSummariesForDefaultStyle);
        }
    }

    private static String expectedReflectionToStringForDefaultStyle(TestObject testObject) {
        return readResource("defaultStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedArrayReflectionToStringForDefaultStyle(TestObject[] testArray) {
        return readResource("defaultStyle.reflectionToString.array.expected")
                .replace("<<TESTARRAY>>", ObjectUtils.identityToString(testArray));
    }

    private static String expectedToStringBuilderWithObfuscateSummariesForDefaultStyle(TestObject testObject) {
        return readResource("defaultStyle.ToStringBuilder.withObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForDefaultStyle(TestObject testObject) {
        return readResource("defaultStyle.ToStringBuilder.withoutObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    @Nested
    @DisplayName("multiLineStyle()")
    class MultiLineStyle extends ToStringStyleTest {

        MultiLineStyle() {
            super(ObfuscatingToStringStyle::multiLineStyle,
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForMultiLineStyle,
                    ObfuscatingToStringStyleTest::expectedArrayReflectionToStringForMultiLineStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithObfuscateSummariesForMultiLineStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithoutObfuscateSummariesForMultiLineStyle);
        }

        @Override
        String getSuperValue() {
            return "[" + System.lineSeparator() + "  super" + System.lineSeparator() + "]";
        }

        @Override
        String getToStringValue() {
            return "[" + System.lineSeparator() + "  toString" + System.lineSeparator() + "]";
        }
    }

    private static String expectedReflectionToStringForMultiLineStyle(TestObject testObject) {
        return readResource("multiLineStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedArrayReflectionToStringForMultiLineStyle(TestObject[] testArray) {
        return readResource("multiLineStyle.reflectionToString.array.expected")
                .replace("<<TESTARRAY>>", ObjectUtils.identityToString(testArray));
    }

    private static String expectedToStringBuilderWithObfuscateSummariesForMultiLineStyle(TestObject testObject) {
        return readResource("multiLineStyle.ToStringBuilder.withObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForMultiLineStyle(TestObject testObject) {
        return readResource("multiLineStyle.ToStringBuilder.withoutObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    @Nested
    @DisplayName("noFieldNamesStyle()")
    class NoFieldNamesStyle extends ToStringStyleTest {

        NoFieldNamesStyle() {
            super(ObfuscatingToStringStyle::noFieldNamesStyle,
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForNoFieldNamesStyle,
                    ObfuscatingToStringStyleTest::expectedArrayReflectionToStringForNoFieldNamesStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithSummarForNoFieldNamesStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithoutSummarForNoFieldNamesStyle);
        }
    }

    private static String expectedReflectionToStringForNoFieldNamesStyle(TestObject testObject) {
        return readResource("noFieldNamesStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedArrayReflectionToStringForNoFieldNamesStyle(TestObject[] testArray) {
        return readResource("noFieldNamesStyle.reflectionToString.array.expected")
                .replace("<<TESTARRAY>>", ObjectUtils.identityToString(testArray));
    }

    private static String expectedToStringBuilderWithSummarForNoFieldNamesStyle(TestObject testObject) {
        return readResource("noFieldNamesStyle.ToStringBuilder.withObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedToStringBuilderWithoutSummarForNoFieldNamesStyle(TestObject testObject) {
        return readResource("noFieldNamesStyle.ToStringBuilder.withoutObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    @Nested
    @DisplayName("shortPrefixStyle()")
    class ShortPrefixStyle extends ToStringStyleTest {

        ShortPrefixStyle() {
            super(ObfuscatingToStringStyle::shortPrefixStyle,
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForShortPrefixStyle,
                    ObfuscatingToStringStyleTest::expectedArrayReflectionToStringForShortPrefixStyle,
                    o -> expectedToStringBuilderWithObfuscateSummariesForShortPrefixStyle(),
                    o -> expectedToStringBuilderWithoutObfuscateSummariesForShortPrefixStyle());
        }
    }

    private static String expectedReflectionToStringForShortPrefixStyle(TestObject testObject) {
        return readResource("shortPrefixStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedArrayReflectionToStringForShortPrefixStyle(TestObject[] testArray) {
        return readResource("shortPrefixStyle.reflectionToString.array.expected")
                .replace("<<TESTARRAY>>", ObjectUtils.identityToString(testArray));
    }

    private static String expectedToStringBuilderWithObfuscateSummariesForShortPrefixStyle() {
        return readResource("shortPrefixStyle.ToStringBuilder.withObfuscateSummaries.expected");
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForShortPrefixStyle() {
        return readResource("shortPrefixStyle.ToStringBuilder.withoutObfuscateSummaries.expected");
    }

    @Nested
    @DisplayName("simpleStyle()")
    class SimpleStyle extends ToStringStyleTest {

        SimpleStyle() {
            super(ObfuscatingToStringStyle::simpleStyle,
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForSimpleStyle,
                    ObfuscatingToStringStyleTest::expectedArrayReflectionToStringForSimpleStyle,
                    o -> expectedToStringBuilderWithObfuscateSummariesForSimpleStyle(),
                    o -> expectedToStringBuilderWithoutObfuscateSummariesForSimpleStyle());
        }

        @Override
        String getSuperValue() {
            return "super";
        }

        @Override
        String getToStringValue() {
            return "toString";
        }
    }

    private static String expectedReflectionToStringForSimpleStyle(TestObject testObject) {
        return readResource("simpleStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", testObject.toString());
    }

    private static String expectedArrayReflectionToStringForSimpleStyle(TestObject[] testArray) {
        return readResource("simpleStyle.reflectionToString.array.expected")
                .replace("<<TESTARRAY>>", testArray.toString());
    }

    private static String expectedToStringBuilderWithObfuscateSummariesForSimpleStyle() {
        return readResource("simpleStyle.ToStringBuilder.withObfuscateSummaries.expected");
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForSimpleStyle() {
        return readResource("simpleStyle.ToStringBuilder.withoutObfuscateSummaries.expected");
    }

    @Nested
    @DisplayName("noClassNameStyle()")
    class NoClassNameStyle extends ToStringStyleTest {

        NoClassNameStyle() {
            super(ObfuscatingToStringStyle::noClassNameStyle,
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForNoClassNameStyle,
                    ObfuscatingToStringStyleTest::expectedArrayReflectionToStringForNoClassNameStyle,
                    o -> expectedToStringBuilderWithObfuscateSummariesForNoClassNameStyle(),
                    o -> expectedToStringBuilderWithoutObfuscateSummariesForNoClassNameStyle());
        }
    }

    private static String expectedReflectionToStringForNoClassNameStyle(TestObject testObject) {
        return readResource("noClassNameStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", testObject.toString());
    }

    private static String expectedArrayReflectionToStringForNoClassNameStyle(TestObject[] testArray) {
        return readResource("noClassNameStyle.reflectionToString.array.expected")
                .replace("<<TESTOBJECT>>", testArray.toString());
    }

    private static String expectedToStringBuilderWithObfuscateSummariesForNoClassNameStyle() {
        return readResource("noClassNameStyle.ToStringBuilder.withObfuscateSummaries.expected");
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForNoClassNameStyle() {
        return readResource("noClassNameStyle.ToStringBuilder.withoutObfuscateSummaries.expected");
    }

    @Nested
    @DisplayName("recursiveStyle()")
    class RecursiveStyle extends ToStringStyleTest {

        RecursiveStyle() {
            super(() -> recursiveStyle(c -> c != Date.class),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForRecursiveStyle,
                    ObfuscatingToStringStyleTest::expectedArrayReflectionToStringForRecursiveStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithObfuscateSummariesForRecursiveStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithoutObfuscateSummariesForRecursiveStyle);
        }
    }

    private static String expectedReflectionToStringForRecursiveStyle(TestObject testObject) {
        return readResource("recursiveStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject))
                .replace("<<NESTED>>", ObjectUtils.identityToString(testObject.nested))
                .replace("<<NOTOBFUSCATED>>", ObjectUtils.identityToString(testObject.notObfuscated));
    }

    private static String expectedArrayReflectionToStringForRecursiveStyle(TestObject[] testArray) {
        return readResource("recursiveStyle.reflectionToString.array.expected")
                .replace("<<TESTARRAY>>", ObjectUtils.identityToString(testArray))
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testArray[0]))
                .replace("<<NESTED>>", ObjectUtils.identityToString(testArray[0].nested))
                .replace("<<NOTOBFUSCATED>>", ObjectUtils.identityToString(testArray[0].notObfuscated));
    }

    private static String expectedToStringBuilderWithObfuscateSummariesForRecursiveStyle(TestObject testObject) {
        return readResource("recursiveStyle.ToStringBuilder.withObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForRecursiveStyle(TestObject testObject) {
        return readResource("recursiveStyle.ToStringBuilder.withoutObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    @Nested
    @DisplayName("multiLineRecursiveStyle()")
    class MultiLineRecursiveStyle extends ToStringStyleTest {

        MultiLineRecursiveStyle() {
            super(() -> multiLineRecursiveStyle(c -> c != Date.class),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForMultiLineRecursiveStyle,
                    ObfuscatingToStringStyleTest::expectedArrayReflectionToStringForMultiLineRecursiveStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithObfuscateSummariesForMultiLineRecursiveStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithoutObfuscateSummariesForMultiLineRecursiveStyle);
        }

        @Override
        String getSuperValue() {
            return "[" + System.lineSeparator() + "  super" + System.lineSeparator() + "]";
        }

        @Override
        String getToStringValue() {
            return "[" + System.lineSeparator() + "  toString" + System.lineSeparator() + "]";
        }
    }

    private static String expectedReflectionToStringForMultiLineRecursiveStyle(TestObject testObject) {
        return readResource("multiLineRecursiveStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject))
                .replace("<<NESTED>>", ObjectUtils.identityToString(testObject.nested))
                .replace("<<NOTOBFUSCATED>>", ObjectUtils.identityToString(testObject.notObfuscated));
    }

    private static String expectedArrayReflectionToStringForMultiLineRecursiveStyle(TestObject[] testArray) {
        return readResource("multiLineRecursiveStyle.reflectionToString.array.expected")
                .replace("<<TESTARRAY>>", ObjectUtils.identityToString(testArray))
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testArray[0]))
                .replace("<<NESTED>>", ObjectUtils.identityToString(testArray[0].nested))
                .replace("<<NOTOBFUSCATED>>", ObjectUtils.identityToString(testArray[0].notObfuscated));
    }

    private static String expectedToStringBuilderWithObfuscateSummariesForMultiLineRecursiveStyle(TestObject testObject) {
        return readResource("multiLineRecursiveStyle.ToStringBuilder.withObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForMultiLineRecursiveStyle(TestObject testObject) {
        return readResource("multiLineRecursiveStyle.ToStringBuilder.withoutObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    static class ToStringStyleTest {

        private Supplier<Builder> builderSupplier;

        private final Function<TestObject, String> expectedReflectionToString;
        private final Function<TestObject[], String> expectedArrayReflectionToString;
        private final Function<TestObject, String> expectedToStringBuilderWithObfuscateSummaries;
        private final Function<TestObject, String> expectedToStringBuilderWithoutObfuscateSummaries;

        ToStringStyleTest(Supplier<Builder> builderSupplier,
                Function<TestObject, String> expectedReflectionToString,
                Function<TestObject[], String> expectedArrayReflectionToString,
                Function<TestObject, String> expectedToStringBuilderWithObfuscateSummaries,
                Function<TestObject, String> expectedToStringBuilderWithoutObfuscateSummaries) {

            this.builderSupplier = builderSupplier;
            this.expectedReflectionToString = expectedReflectionToString;
            this.expectedArrayReflectionToString = expectedArrayReflectionToString;
            this.expectedToStringBuilderWithObfuscateSummaries = expectedToStringBuilderWithObfuscateSummaries;
            this.expectedToStringBuilderWithoutObfuscateSummaries = expectedToStringBuilderWithoutObfuscateSummaries;
        }

        @Test
        @DisplayName("reflectionToString")
        void testReflectionToString() {
            TestObject testObject = new TestObject();
            testObject.nested = new TestObject();
            testObject.notObfuscated = new TestObject();
            String string = ToStringBuilder.reflectionToString(testObject, configureBuilder(builderSupplier.get()).build());
            assertEquals(expectedReflectionToString.apply(testObject).replace("\r", ""), string.replace("\r", ""));

            string = ToStringBuilder.reflectionToString(testObject, configureBuilder(builderSupplier.get()).supplier().get());
            assertEquals(expectedReflectionToString.apply(testObject).replace("\r", ""), string.replace("\r", ""));
        }

        @Test
        @DisplayName("reflectionAppendArrayDetail")
        void testReflectionAppendArrayDetail() {
            TestObject testObject = new TestObject();
            testObject.nested = new TestObject();
            testObject.notObfuscated = new TestObject();
            TestObject[] testArray = { testObject };
            String string = ToStringBuilder.reflectionToString(testArray, configureBuilder(builderSupplier.get()).build());
            assertEquals(expectedArrayReflectionToString.apply(testArray).replace("\r", ""), string.replace("\r", ""));

            string = ToStringBuilder.reflectionToString(testArray, configureBuilder(builderSupplier.get()).supplier().get());
            assertEquals(expectedArrayReflectionToString.apply(testArray).replace("\r", ""), string.replace("\r", ""));
        }

        @Nested
        @DisplayName("ToStringBuilder")
        class ToStringBuilderTest {

            @Test
            @DisplayName("withObfuscateSummaries(true)")
            void testWithObfuscateSummaries() {
                TestObject testObject = new TestObject();
                String string = createTestString(testObject, true);

                assertEquals(expectedToStringBuilderWithObfuscateSummaries.apply(testObject).replace("\r", ""), string.replace("\r", ""));
            }

            @Test
            @DisplayName("withObfuscateSummaries(false)")
            void testWithoutObfuscateSummaries() {
                TestObject testObject = new TestObject();
                String string = createTestString(testObject, false);

                assertEquals(expectedToStringBuilderWithoutObfuscateSummaries.apply(testObject).replace("\r", ""), string.replace("\r", ""));
            }

            private String createTestString(TestObject testObject, boolean obfuscateSummaries) {
                Builder builder = builderSupplier.get();
                builder = obfuscateSummaries ? builder.includeSummariesByDefault() : builder.excludeSummariesByDefault();
                builder = configureBuilder(builder);

                return new ToStringBuilder(testObject, builder.build())
                        .append(testObject.booleanValue)
                        .append(testObject.booleanArray)
                        .append(testObject.byteValue)
                        .append(testObject.byteArray)
                        .append(testObject.charValue)
                        .append(testObject.charArray)
                        .append(testObject.doubleValue)
                        .append(testObject.doubleArray)
                        .append(testObject.floatValue)
                        .append(testObject.floatArray)
                        .append(testObject.intValue)
                        .append(testObject.intArray)
                        .append(testObject.longValue)
                        .append(testObject.longArray)
                        .append(testObject.stringValue)
                        .append(testObject.dateValue)
                        .append(testObject.stringList)
                        .append(testObject.intMap)
                        .append(testObject.nullValue)
                        .append(testObject.objectArray)
                        .append(testObject.shortValue)
                        .append(testObject.shortArray)
                        // omit appending with fields except with explicitly no details
                        .append("booleanArray", testObject.booleanArray, false)
                        .append("byteArray", testObject.byteArray, false)
                        .append("charArray", testObject.charArray, false)
                        .append("doubleArray", testObject.doubleArray, false)
                        .append("floatArray", testObject.floatArray, false)
                        .append("intArray", testObject.intArray, false)
                        .append("longArray", testObject.longArray, false)
                        .append("stringValue", testObject.stringValue, false)
                        .append("dateValue", testObject.dateValue, false)
                        .append("stringList", testObject.stringList, false)
                        .append("intMap", testObject.intMap, false)
                        .append("nullValue", testObject.nullValue, false)
                        .append("objectArray", testObject.objectArray, false)
                        .append("shortArray", testObject.shortArray, false)
                        .append("notMatchedBooleanArray", testObject.notMatchedBooleanArray, false)
                        .append("notMatchedByteArray", testObject.notMatchedByteArray, false)
                        .append("notMatchedCharArray", testObject.notMatchedCharArray, false)
                        .append("notMatchedDoubleArray", testObject.notMatchedDoubleArray, false)
                        .append("notMatchedFloatArray", testObject.notMatchedFloatArray, false)
                        .append("notMatchedIntArray", testObject.notMatchedIntArray, false)
                        .append("notMatchedLongArray", testObject.notMatchedLongArray, false)
                        .append("notMatchedStringValue", testObject.notMatchedStringValue, false)
                        .append("notMatchedDateValue", testObject.notMatchedDateValue, false)
                        .append("notMatchedStringList", testObject.notMatchedStringList, false)
                        .append("notMatchedIntMap", testObject.notMatchedIntMap, false)
                        .append("notMatchedNullValue", testObject.notMatchedNullValue, false)
                        .append("notMatchedObjectArray", testObject.notMatchedObjectArray, false)
                        .append("notMatchedShortArray", testObject.notMatchedShortArray, false)
                        .appendSuper(getSuperValue())
                        .appendToString(getToStringValue())
                        .toString();
            }
        }

        String getSuperValue() {
            return "[super]";
        }

        String getToStringValue() {
            return "[toString]";
        }
    }

    private static Builder configureBuilder(Builder builder) {
        Obfuscator obfuscator = fixedLength(3);
        return builder
                .withField("stringValue", obfuscator)
                .withField("dateValue", obfuscator)
                .withField("stringList", obfuscator)
                .withField("intMap", obfuscator)
                .withField("longValue", obfuscator)
                .withField("intValue", obfuscator)
                .withField("shortValue", obfuscator)
                .withField("byteValue", obfuscator)
                .withField("charValue", obfuscator)
                .withField("doubleValue", obfuscator)
                .withField("floatValue", obfuscator)
                .withField("booleanValue", obfuscator)
                .withField("objectArray", obfuscator)
                .withField("longArray", obfuscator)
                .withField("intArray", obfuscator)
                .withField("shortArray", obfuscator)
                .withField("byteArray", obfuscator)
                .withField("charArray", obfuscator)
                .withField("doubleArray", obfuscator)
                .withField("floatArray", obfuscator)
                .withField("booleanArray", obfuscator)
                .withField("nullValue", obfuscator)
                .withField("notObfuscated", none())
                ;
    }

    @Nested
    @DisplayName("Builder")
    class BuilderTest {

        @Test
        @DisplayName("transform")
        void testTransform() {
            Builder builder = defaultStyle();
            @SuppressWarnings("unchecked")
            Function<Builder, String> f = mock(Function.class);
            when(f.apply(builder)).thenReturn("result");

            assertEquals("result", builder.transform(f));
            verify(f).apply(builder);
            verifyNoMoreInteractions(f);
        }

        @Nested
        @DisplayName("case sensitivity")
        class CaseSensitivity {

            @Test
            @DisplayName("caseSensitiveByDefault and overridden")
            void testCaseSensitiveByDefaultAndOverridden() {
                ObfuscatingToStringStyle toStringStyle = defaultStyle()
                        .caseSensitiveByDefault()
                        .withField("caseSensitive", fixedLength(3))
                        .withField("caseInsensitive", fixedLength(3), CASE_INSENSITIVE)
                        .build();

                StringBuffer buffer = new StringBuffer();
                toStringStyle.append(buffer, "caseSensitive", "value", true);
                assertEquals("caseSensitive=***,", buffer.toString());

                buffer.delete(0, buffer.length());
                toStringStyle.append(buffer, "CASESENSITIVE", "value", true);
                assertEquals("CASESENSITIVE=value,", buffer.toString());

                buffer.delete(0, buffer.length());
                toStringStyle.append(buffer, "caseInsensitive", "value", true);
                assertEquals("caseInsensitive=***,", buffer.toString());

                buffer.delete(0, buffer.length());
                toStringStyle.append(buffer, "CASEINSENSITIVE", "value", true);
                assertEquals("CASEINSENSITIVE=***,", buffer.toString());
            }

            @Test
            @DisplayName("caseInsensitiveByDefault and overridden")
            void testCaseInsensitiveByDefaultAndOverridden() {
                ObfuscatingToStringStyle toStringStyle = defaultStyle()
                        .caseInsensitiveByDefault()
                        .withField("caseInsensitive", fixedLength(3))
                        .withField("caseSensitive", fixedLength(3), CASE_SENSITIVE)
                        .build();

                StringBuffer buffer = new StringBuffer();
                toStringStyle.append(buffer, "caseInsensitive", "value", true);
                assertEquals("caseInsensitive=***,", buffer.toString());

                buffer.delete(0, buffer.length());
                toStringStyle.append(buffer, "CASEINSENSITIVE", "value", true);
                assertEquals("CASEINSENSITIVE=***,", buffer.toString());

                buffer.delete(0, buffer.length());
                toStringStyle.append(buffer, "caseSensitive", "value", true);
                assertEquals("caseSensitive=***,", buffer.toString());

                buffer.delete(0, buffer.length());
                toStringStyle.append(buffer, "CASESENSITIVE", "value", true);
                assertEquals("CASESENSITIVE=value,", buffer.toString());
            }
        }

        @Nested
        @DisplayName("summaries")
        class Summaries {

            @Test
            @DisplayName("excludeByDefault and overridden")
            void testExcludedByDefaultAndOverridden() {
                ObfuscatingToStringStyle toStringStyle = defaultStyle()
                        .excludeSummariesByDefault()
                        .withField("excluded", fixedLength(3))
                        .withField("included", fixedLength(3))
                                .includeSummaries()
                        .build();

                StringBuffer buffer = new StringBuffer();
                toStringStyle.append(buffer, "excluded", "value", false);
                assertEquals("excluded=<String>,", buffer.toString());

                buffer.delete(0, buffer.length());
                toStringStyle.append(buffer, "included", "value", true);
                assertEquals("included=***,", buffer.toString());
            }

            @Test
            @DisplayName("includedByDefault and overridden")
            void testIncludedByDefaultAndOverridden() {
                ObfuscatingToStringStyle toStringStyle = defaultStyle()
                        .includeSummariesByDefault()
                        .withField("included", fixedLength(3))
                        .withField("excluded", fixedLength(3))
                                .excludeSummaries()
                        .build();

                StringBuffer buffer = new StringBuffer();
                toStringStyle.append(buffer, "included", "value", true);
                assertEquals("included=***,", buffer.toString());

                buffer.delete(0, buffer.length());
                toStringStyle.append(buffer, "excluded", "value", false);
                assertEquals("excluded=<String>,", buffer.toString());
            }
        }
    }

    private static String readResource(String name) {
        StringBuilder sb = new StringBuilder();
        try (Reader input = new InputStreamReader(ObfuscatingToStringStyleTest.class.getResourceAsStream(name), StandardCharsets.UTF_8)) {
            char[] buffer = new char[4096];
            int len;
            while ((len = input.read(buffer)) != -1) {
                sb.append(buffer, 0, len);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString().replace("\r\n", "\n");
    }

    @SuppressWarnings("unused")
    private static final class TestObject {

        private final String stringValue = "string\",int";
        private final Date dateValue = new Date(0);

        private final List<String> stringList = Arrays.asList("foo", "bar", null);
        private final Map<String, Integer> intMap = testMap();

        private final long longValue = 1;
        private final int intValue = 2;
        private final short shortValue = 3;
        private final byte byteValue = 4;
        private final char charValue = 'A';
        private final double doubleValue = Math.PI;
        private final float floatValue = (float) Math.E;
        private final boolean booleanValue = true;

        private final Object[] objectArray = { 1, "foo", null };

        private final long[] longArray = { 1, 2 };
        private final int[] intArray = { 3, 4 };
        private final short[] shortArray = { 5, 6 };
        private final byte[] byteArray = { 7, 8 };
        private final char[] charArray = { 'B', 'C' };
        private final double[] doubleArray = { Math.PI, Math.E };
        private final float[] floatArray = { (float) (Math.PI / 2), (float) (Math.E / 2) };
        private final boolean[] booleanArray = { true, false };

        private final Object nullValue = null;

        private final String notMatchedStringValue = "string\",int";
        private final Date notMatchedDateValue = new Date(0);

        private final List<String> notMatchedStringList = Arrays.asList("foo", "bar", null);
        private final Map<String, Integer> notMatchedIntMap = testMap();

        private final long notMatchedLongValue = 1;
        private final int notMatchedIntValue = 2;
        private final short notMatchedShortValue = 3;
        private final byte notMatchedByteValue = 4;
        private final char notMatchedCharValue = 'A';
        private final double notMatchedDoubleValue = Math.PI;
        private final float notMatchedFloatValue = (float) Math.E;
        private final boolean notMatchedBooleanValue = true;

        private final Object[] notMatchedObjectArray = { 1, "foo", null };

        private final long[] notMatchedLongArray = { 1, 2 };
        private final int[] notMatchedIntArray = { 3, 4 };
        private final short[] notMatchedShortArray = { 5, 6 };
        private final byte[] notMatchedByteArray = { 7, 8 };
        private final char[] notMatchedCharArray = { 'B', 'C' };
        private final double[] notMatchedDoubleArray = { Math.PI, Math.E };
        private final float[] notMatchedFloatArray = { (float) (Math.PI / 2), (float) (Math.E / 2) };
        private final boolean[] notMatchedBooleanArray = { true, false };

        private final Object notMatchedNullValue = null;

        private TestObject nested = null;

        private TestObject notObfuscated = null;

        private final TestObject self = this;

        private Map<String, Integer> testMap() {
            Map<String, Integer> map = new LinkedHashMap<>();
            map.put("foo", 13);
            map.put("bar", 14);
            map.put(null, null);
            return map;
        }

        @Override
        public String toString() {
            return "<TestObject>";
        }
    }
}
