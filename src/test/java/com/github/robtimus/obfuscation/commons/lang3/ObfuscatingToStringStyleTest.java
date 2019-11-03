/*
 * ObfuscatingToStringStyleTest.java
 * Copyright 2019 Rob Spoor
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
import static com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.multiLineStyle;
import static com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.noClassNameStyle;
import static com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.noFieldNamesStyle;
import static com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.recursiveStyle;
import static com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.shortPrefixStyle;
import static com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.simpleStyle;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

@SuppressWarnings({ "javadoc", "nls" })
public class ObfuscatingToStringStyleTest {

    @Nested
    @DisplayName("defaultStyle()")
    public class DefaultStyle extends ToStringStyleTest {

        public DefaultStyle() {
            super(() -> configureBuilder(defaultStyle()),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForDefaultStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithObfuscateSummariesForDefaultStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithoutObfuscateSummariesForDefaultStyle);
        }
    }

    private static String expectedReflectionToStringForDefaultStyle(TestObject testObject) {
        return readResource("defaultStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
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
    public class MultiLineStyle extends ToStringStyleTest {

        public MultiLineStyle() {
            super(() -> configureBuilder(multiLineStyle()),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForMultiLineStyle,
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
    public class NoFieldNamesStyle extends ToStringStyleTest {

        public NoFieldNamesStyle() {
            super(() -> configureBuilder(noFieldNamesStyle()),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForNoFieldNamesStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithSummarForNoFieldNamesStyle,
                    ObfuscatingToStringStyleTest::expectedToStringBuilderWithoutSummarForNoFieldNamesStyle);
        }
    }

    private static String expectedReflectionToStringForNoFieldNamesStyle(TestObject testObject) {
        return readResource("noFieldNamesStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
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
    public class ShortPrefixStyle extends ToStringStyleTest {

        public ShortPrefixStyle() {
            super(() -> configureBuilder(shortPrefixStyle()),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForShortPrefixStyle,
                    o -> expectedToStringBuilderWithObfuscateSummariesForShortPrefixStyle(),
                    o -> expectedToStringBuilderWithoutObfuscateSummariesForShortPrefixStyle());
        }
    }

    private static String expectedReflectionToStringForShortPrefixStyle(TestObject testObject) {
        return readResource("shortPrefixStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedToStringBuilderWithObfuscateSummariesForShortPrefixStyle() {
        return readResource("shortPrefixStyle.ToStringBuilder.withObfuscateSummaries.expected");
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForShortPrefixStyle() {
        return readResource("shortPrefixStyle.ToStringBuilder.withoutObfuscateSummaries.expected");
    }

    @Nested
    @DisplayName("simpleStyle()")
    public class SimpleStyle extends ToStringStyleTest {

        public SimpleStyle() {
            super(() -> configureBuilder(simpleStyle()),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForSimpleStyle,
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

    private static String expectedToStringBuilderWithObfuscateSummariesForSimpleStyle() {
        return readResource("simpleStyle.ToStringBuilder.withObfuscateSummaries.expected");
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForSimpleStyle() {
        return readResource("simpleStyle.ToStringBuilder.withoutObfuscateSummaries.expected");
    }

    @Nested
    @DisplayName("noClassNameStyle()")
    public class NoClassNameStyle extends ToStringStyleTest {

        public NoClassNameStyle() {
            super(() -> configureBuilder(noClassNameStyle()),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForNoClassNameStyle,
                    o -> expectedToStringBuilderWithObfuscateSummariesForNoClassNameStyle(),
                    o -> expectedToStringBuilderWithoutObfuscateSummariesForNoClassNameStyle());
        }
    }

    private static String expectedReflectionToStringForNoClassNameStyle(TestObject testObject) {
        return readResource("noClassNameStyle.reflectionToString.expected")
                .replace("<<TESTOBJECT>>", testObject.toString());
    }

    private static String expectedToStringBuilderWithObfuscateSummariesForNoClassNameStyle() {
        return readResource("noClassNameStyle.ToStringBuilder.withObfuscateSummaries.expected");
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForNoClassNameStyle() {
        return readResource("noClassNameStyle.ToStringBuilder.withoutObfuscateSummaries.expected");
    }

    @Nested
    @DisplayName("recursiveStyle()")
    public class RecursiveStyle extends ToStringStyleTest {

        public RecursiveStyle() {
            super(() -> configureBuilder(recursiveStyle(c -> c != Date.class)),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForRecursiveStyle,
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
    public class MultiLineRecursiveStyle extends ToStringStyleTest {

        public MultiLineRecursiveStyle() {
            super(() -> configureBuilder(multiLineRecursiveStyle(c -> c != Date.class)),
                    ObfuscatingToStringStyleTest::expectedReflectionToStringForMultiLineRecursiveStyle,
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

    private static String expectedToStringBuilderWithObfuscateSummariesForMultiLineRecursiveStyle(TestObject testObject) {
        return readResource("multiLineRecursiveStyle.ToStringBuilder.withObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static String expectedToStringBuilderWithoutObfuscateSummariesForMultiLineRecursiveStyle(TestObject testObject) {
        return readResource("multiLineRecursiveStyle.ToStringBuilder.withoutObfuscateSummaries.expected")
                .replace("<<TESTOBJECT>>", ObjectUtils.identityToString(testObject));
    }

    private static class ToStringStyleTest {

        private Supplier<Builder> builderSupplier;

        private final Function<TestObject, String> expectedReflectionToString;
        private final Function<TestObject, String> expectedToStringBuilderWithObfuscateSummaries;
        private final Function<TestObject, String> expectedToStringBuilderWithoutObfuscateSummaries;

        ToStringStyleTest(Supplier<Builder> builderSupplier,
                Function<TestObject, String> expectedReflectionToString,
                Function<TestObject, String> expectedToStringBuilderWithObfuscateSummaries,
                Function<TestObject, String> expectedToStringBuilderWithoutObfuscateSummaries) {

            this.builderSupplier = builderSupplier;
            this.expectedReflectionToString = expectedReflectionToString;
            this.expectedToStringBuilderWithObfuscateSummaries = expectedToStringBuilderWithObfuscateSummaries;
            this.expectedToStringBuilderWithoutObfuscateSummaries = expectedToStringBuilderWithoutObfuscateSummaries;
        }

        @Test
        @DisplayName("reflectionToString")
        public void testReflectionToString() {
            TestObject testObject = new TestObject();
            testObject.nested = new TestObject();
            testObject.notObfuscated = new TestObject();
            String string = ToStringBuilder.reflectionToString(testObject, builderSupplier.get().build().get());
            assertEquals(expectedReflectionToString.apply(testObject).replace("\r", ""), string.replace("\r", ""));
        }

        @Nested
        @DisplayName("ToStringBuilder")
        public class ToStringBuilderTest {

            @Test
            @DisplayName("withObfuscateSummaries(true)")
            public void testWithObfuscateSummaries() {
                TestObject testObject = new TestObject();
                String string = createTestString(testObject, true);

                assertEquals(expectedToStringBuilderWithObfuscateSummaries.apply(testObject).replace("\r", ""), string.replace("\r", ""));
            }

            @Test
            @DisplayName("withObfuscateSummaries(false)")
            public void testWithoutObfuscateSummaries() {
                TestObject testObject = new TestObject();
                String string = createTestString(testObject, false);

                assertEquals(expectedToStringBuilderWithoutObfuscateSummaries.apply(testObject).replace("\r", ""), string.replace("\r", ""));
            }

            private String createTestString(TestObject testObject, boolean withObfuscateSummaries) {
                return new ToStringBuilder(testObject, builderSupplier.get().withObfuscatedSummaries(withObfuscateSummaries).build().get())
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
                .withProperty("stringValue", obfuscator)
                .withProperty("dateValue", obfuscator)
                .withProperty("stringList", obfuscator)
                .withProperty("intMap", obfuscator)
                .withProperty("longValue", obfuscator)
                .withProperty("intValue", obfuscator)
                .withProperty("shortValue", obfuscator)
                .withProperty("byteValue", obfuscator)
                .withProperty("charValue", obfuscator)
                .withProperty("doubleValue", obfuscator)
                .withProperty("floatValue", obfuscator)
                .withProperty("booleanValue", obfuscator)
                .withProperty("objectArray", obfuscator)
                .withProperty("longArray", obfuscator)
                .withProperty("intArray", obfuscator)
                .withProperty("shortArray", obfuscator)
                .withProperty("byteArray", obfuscator)
                .withProperty("charArray", obfuscator)
                .withProperty("doubleArray", obfuscator)
                .withProperty("floatArray", obfuscator)
                .withProperty("booleanArray", obfuscator)
                .withProperty("nullValue", obfuscator)
                .withProperty("notObfuscated", none())
                ;
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
        return sb.toString();
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
