/*
 * ObfuscatingToStringStyle.java
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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.PropertyAwareBuilder;

/**
 * A {@link ToStringStyle} that can obfuscate fields.
 * <p>
 * An {@link ObfuscatingToStringStyle} object is not thread safe nor stateless, and should not be reused for multiple objects.
 * However, a {@link Builder} can be safely reused for multiple objects and in multiple threads as long as only its {@link Builder#build()} method is
 * called. For instance, in a class:
 * <pre><code>
 * private static final ObfuscatingToStringStyle.Builder TO_STRING_STYLE_BUILDER = ...;
 *
 * ...
 *
 * public String toString() {
 *     return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE_BUILDER.build());
 * }
 * </code></pre>
 * <p>
 * Note: instances of {@code ObfuscatingToStringStyle} are <b>not</b> serializable, because obfuscators (in general) aren't.
 *
 * @author Rob Spoor
 */
public abstract class ObfuscatingToStringStyle extends ToStringStyle {

    private static final long serialVersionUID = 1L;

    private static final int OBFUSCATED_TEXT_BUFFER_SIZE = 32;

    private final Map<String, Obfuscator> obfuscators;
    private final boolean obfuscateSummaries;

    // obfuscator != null means the current value needs be obfuscated.
    private Obfuscator obfuscator;

    private final StringBuffer obfuscatedText;

    /**
     * Creates a new obfuscating {@link ToStringStyle} based on the settings of a builder.
     *
     * @param builder The builder with the settings for this obfuscating {@link ToStringStyle}.
     */
    protected ObfuscatingToStringStyle(Builder builder) {
        obfuscators = builder.obfuscators();
        obfuscateSummaries = builder.obfuscateSummaries;

        obfuscator = null;
        obfuscatedText = new StringBuffer(OBFUSCATED_TEXT_BUFFER_SIZE);
    }

    private void appendAndClearObfuscatedText(StringBuffer buffer) {
        buffer.append(obfuscator.obfuscateText(obfuscatedText));
        obfuscatedText.delete(0, obfuscatedText.length());
    }

    final void doAppend(StringBuffer buffer, String fieldName, Consumer<StringBuffer> append) {
        if (obfuscator == null) {
            obfuscator = fieldName == null ? null : obfuscators.get(fieldName);
            if (obfuscator != null) {
                // start obfuscating; append to obfuscatedText, then append that to buffer when done
                append.accept(obfuscatedText);
                appendAndClearObfuscatedText(buffer);
                obfuscator = null;
            } else {
                // no need to obfuscate this field, append to buffer
                append.accept(buffer);
            }
        } else {
            // already obfuscating, append to obfuscatedText instead of buffer
            append.accept(obfuscatedText);
        }
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, value));
    }

    @Override
    protected void appendSummary(StringBuffer buffer, String fieldName, Object value) {
        if (obfuscateSummaries) {
            doAppend(buffer, fieldName, b -> super.appendSummary(b, fieldName, value));
        } else {
            super.appendSummary(buffer, fieldName, value);
        }
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, Collection<?> coll) {
        // treat collections the same way as arrays; don't simply append to the StringBuffer
        doAppend(buffer, fieldName, b -> {
            b.append(getArrayStart().replace('{', '['));
            for (Iterator<?> i = coll.iterator(); i.hasNext(); ) {
                final Object item = i.next();
                if (item == null) {
                    appendNullText(b, fieldName);
                } else {
                    appendInternal(b, fieldName, item, isArrayContentDetail());
                }
                if (i.hasNext()) {
                    b.append(getArraySeparator());
                }
            }
            b.append(getArrayEnd().replace('}', ']'));
        });
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, Map<?, ?> map) {
        // treat maps the same way as arrays; don't simply append to the StringBuffer
        doAppend(buffer, fieldName, b -> {
            b.append(getArrayStart());
            for (Iterator<?> i = map.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) i.next();

                // append the key as null or default; complex keys would make reading very hard
                final Object key = entry.getKey();
                if (key == null) {
                    appendNullText(b, fieldName);
                } else {
                    b.append(key);
                }

                b.append('=');

                final Object value = entry.getValue();
                if (value == null) {
                    appendNullText(b, fieldName);
                } else {
                    appendInternal(b, fieldName, value, isArrayContentDetail());
                }

                if (i.hasNext()) {
                    b.append(getArraySeparator());
                }
            }
            b.append(getArrayEnd());
        });
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, long value) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, value));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, int value) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, value));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, short value) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, value));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, byte value) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, value));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, char value) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, value));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, double value) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, value));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, float value) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, value));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, boolean value) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, value));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, Object[] array) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, array));
    }

    @Override
    protected void reflectionAppendArrayDetail(StringBuffer buffer, String fieldName, Object array) {
        doAppend(buffer, fieldName, b -> super.reflectionAppendArrayDetail(b, fieldName, array));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, long[] array) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, array));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, int[] array) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, array));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, short[] array) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, array));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, byte[] array) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, array));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, char[] array) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, array));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, double[] array) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, array));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, float[] array) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, array));
    }

    @Override
    protected void appendDetail(StringBuffer buffer, String fieldName, boolean[] array) {
        doAppend(buffer, fieldName, b -> super.appendDetail(b, fieldName, array));
    }

    @Override
    protected void appendNullText(StringBuffer buffer, String fieldName) {
        doAppend(buffer, fieldName, b -> super.appendNullText(b, fieldName));
    }

    @Override
    protected void appendSummarySize(StringBuffer buffer, String fieldName, int size) {
        if (obfuscateSummaries) {
            doAppend(buffer, fieldName, b -> super.appendSummarySize(b, fieldName, size));
        } else {
            super.appendSummarySize(buffer, fieldName, size);
        }
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to {@link ToStringStyle#DEFAULT_STYLE}.
     *
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to {@link ToStringStyle#DEFAULT_STYLE}.
     */
    public static Builder defaultStyle() {
        return new Builder(DefaultObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     * {@link ToStringStyle#MULTI_LINE_STYLE}.
     *
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     *         {@link ToStringStyle#MULTI_LINE_STYLE}.
     */
    public static Builder multiLineStyle() {
        return new Builder(MultiLineObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     * {@link ToStringStyle#NO_FIELD_NAMES_STYLE}.
     *
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     *         {@link ToStringStyle#NO_FIELD_NAMES_STYLE}.
     */
    public static Builder noFieldNamesStyle() {
        return new Builder(NoFieldNamesObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     * {@link ToStringStyle#SHORT_PREFIX_STYLE}.
     *
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     *         {@link ToStringStyle#SHORT_PREFIX_STYLE}.
     */
    public static Builder shortPrefixStyle() {
        return new Builder(ShortPrefixObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to {@link ToStringStyle#SIMPLE_STYLE}.
     *
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to {@link ToStringStyle#SIMPLE_STYLE}.
     */
    public static Builder simpleStyle() {
        return new Builder(SimpleObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     * {@link ToStringStyle#NO_CLASS_NAME_STYLE}.
     *
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     *         {@link ToStringStyle#NO_CLASS_NAME_STYLE}.
     */
    public static Builder noClassNameStyle() {
        return new Builder(NoClassNameObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     * {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     * This method is similar to calling {@link #recursiveStyle(Predicate)} with a predicate that always returns {@code true}.
     *
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     *         {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     */
    public static Builder recursiveStyle() {
        return recursiveStyle(c -> true);
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     * {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     *
     * @param recurseIntoPredicate A predicate that determines which classes are recursively formatted.
     *                                 Note that primitive types, primitive wrappers and {@link String} are never recursively formatted.
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     *         {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     */
    public static Builder recursiveStyle(Predicate<? super Class<?>> recurseIntoPredicate) {
        Objects.requireNonNull(recurseIntoPredicate);
        return new Builder(b -> new RecursiveObfuscatingToStringStyle(b, recurseIntoPredicate));
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     * {@link org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle MultilineRecursiveToStringStyle}.
     * This method is similar to calling {@link #recursiveStyle(Predicate)} with a predicate that always returns {@code true}.
     *
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     *         {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     */
    public static Builder multiLineRecursiveStyle() {
        return multiLineRecursiveStyle(c -> true);
    }

    /**
     * Returns a builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     * {@link org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle MultilineRecursiveToStringStyle}.
     *
     * @param recurseIntoPredicate A predicate that determines which classes are recursively formatted.
     *                                 Note that primitive types, primitive wrappers and {@link String} are never recursively formatted.
     * @return A builder that creates an obfuscating {@link ToStringStyle} object that produces output similar to
     *         {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     */
    public static Builder multiLineRecursiveStyle(Predicate<? super Class<?>> recurseIntoPredicate) {
        Objects.requireNonNull(recurseIntoPredicate);
        return new Builder(b -> new MultiLineRecursiveObfuscatingToStringStyle(b, recurseIntoPredicate));
    }

    /**
     * A builder for creating obfuscating {@link ToStringStyle} objects.
     *
     * @author Rob Spoor
     */
    public static final class Builder extends PropertyAwareBuilder<Builder, ObfuscatingToStringStyle> {

        private final Function<? super Builder, ? extends ObfuscatingToStringStyle> factory;

        private boolean obfuscateSummaries = false;

        /**
         * Creates a new builder.
         * <p>
         * This builder will use a factory to build {@link ObfuscatingToStringStyle ObfuscatingToStringStyles} based on this builder.
         * This allows the building of sub classes of {@link ObfuscatingToStringStyle}; just provide a constructor that takes a {@code Builder} which
         * delegates to {@link ObfuscatingToStringStyle#ObfuscatingToStringStyle(Builder)}, and provide this constructor as factory to this
         * constructor.
         *
         * @param factory The factory to build {@link ObfuscatingToStringStyle ObfuscatingToStringStyles} based on this builder.
         * @throws NullPointerException If the given factory is {@code null}.
         */
        public Builder(Function<? super Builder, ? extends ObfuscatingToStringStyle> factory) {
            this.factory = Objects.requireNonNull(factory);
        }

        /**
         * Sets whether or not to obfuscate summaries as well as details. The default is {@code false}.
         *
         * @param obfuscateSummaries {@code true} to obfuscate summaries, {@code false} otherwise.
         * @return This object.
         */
        public Builder withObfuscatedSummaries(boolean obfuscateSummaries) {
            this.obfuscateSummaries = obfuscateSummaries;
            return this;
        }

        @Override
        public ObfuscatingToStringStyle build() {
            return factory.apply(this);
        }
    }

    private static final class DefaultObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private DefaultObfuscatingToStringStyle(Builder builder) {
            super(builder);
            // no modifications
        }
    }

    private static final class MultiLineObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private MultiLineObfuscatingToStringStyle(Builder builder) {
            super(builder);
            setContentStart("["); //$NON-NLS-1$
            setFieldSeparator(System.lineSeparator() + "  "); //$NON-NLS-1$
            setFieldSeparatorAtStart(true);
            setContentEnd(System.lineSeparator() + "]"); //$NON-NLS-1$
        }
    }

    private static final class NoFieldNamesObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private NoFieldNamesObfuscatingToStringStyle(Builder builder) {
            super(builder);
            setUseFieldNames(false);
        }
    }

    private static final class ShortPrefixObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private ShortPrefixObfuscatingToStringStyle(Builder builder) {
            super(builder);
            setUseShortClassName(true);
            setUseIdentityHashCode(false);
        }
    }

    private static final class SimpleObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private SimpleObfuscatingToStringStyle(Builder builder) {
            super(builder);
            setUseClassName(false);
            setUseIdentityHashCode(false);
            setUseFieldNames(false);
            setContentStart(""); //$NON-NLS-1$
            setContentEnd(""); //$NON-NLS-1$
        }
    }

    private static final class NoClassNameObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private NoClassNameObfuscatingToStringStyle(Builder builder) {
            super(builder);
            setUseClassName(false);
            setUseIdentityHashCode(false);
        }
    }

    private static class RecursiveObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private final Predicate<? super Class<?>> recurseIntoPredicate;

        RecursiveObfuscatingToStringStyle(Builder builder, Predicate<? super Class<?>> recurseIntoPredicate) {
            super(builder);

            this.recurseIntoPredicate = Objects.requireNonNull(recurseIntoPredicate);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {
            if (shouldRecurseInto(value)) {
                doAppend(buffer, fieldName, b -> {
                    // ignore the result of toString(), since the new ReflectionToStringBuilder will share the buffer and append directly to it
                    new ReflectionToStringBuilder(value, this, b).toString();
                });
            } else {
                super.appendDetail(buffer, fieldName, value);
            }
        }

        boolean shouldRecurseInto(Object value) {
            Class<?> valueType = value.getClass();
            return !ClassUtils.isPrimitiveWrapper(valueType) && !String.class.equals(valueType) && recurseIntoPredicate.test(valueType);
        }
    }

    private static final class MultiLineRecursiveObfuscatingToStringStyle extends RecursiveObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private static final int INDENT = 2;

        private int currentIndent;

        private MultiLineRecursiveObfuscatingToStringStyle(Builder builder, Predicate<? super Class<?>> recurseIntoPredicate) {
            super(builder, recurseIntoPredicate);

            setIndent(1);
        }

        private void setIndent(int newIndent) {
            currentIndent = newIndent;

            final String lineSeparator = System.lineSeparator();

            setArrayStart(indented('{', lineSeparator, currentIndent));
            setArraySeparator(indented(',', lineSeparator, currentIndent));
            setArrayEnd(indented(lineSeparator, currentIndent - 1, '}'));

            setContentStart(indented('[', lineSeparator, currentIndent));
            setFieldSeparator(indented(',', lineSeparator, currentIndent));
            setContentEnd(indented(lineSeparator, currentIndent - 1, ']'));
        }

        /**
         * @return prefix + line separator + indent
         */
        private String indented(char prefix, String lineSeparator, int indentLevel) {
            StringBuilder sb = new StringBuilder(1 + lineSeparator.length() + indentLevel * INDENT);
            sb.append(prefix);
            sb.append(lineSeparator);
            indent(sb, indentLevel);
            return sb.toString();
        }

        /**
         * @return line separator + indent + postfix
         */
        private String indented(String lineSeparator, int indentLevel, char postfix) {
            StringBuilder sb = new StringBuilder(lineSeparator.length() + indentLevel * INDENT + 1);
            sb.append(lineSeparator);
            indent(sb, indentLevel);
            sb.append(postfix);
            return sb.toString();
        }

        private void indent(StringBuilder sb, int indentLevel) {
            for (int i = 0, count = indentLevel * INDENT; i < count; i++) {
                sb.append(' ');
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, Object value) {
            if (shouldRecurseInto(value)) {
                setIndent(currentIndent + 1);
                doAppend(buffer, fieldName, b -> {
                    // ignore the result of toString(), since the new ReflectionToStringBuilder will share the buffer and append directly to it
                    new ReflectionToStringBuilder(value, this, b).toString();
                });
                setIndent(currentIndent - 1);
            } else {
                super.appendDetail(buffer, fieldName, value);
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, Collection<?> coll) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, coll);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, Map<?, ?> map) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, map);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, Object[] array) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void reflectionAppendArrayDetail(StringBuffer buffer, String fieldName, Object array) {
            setIndent(currentIndent + 1);
            super.reflectionAppendArrayDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, long[] array) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, int[] array) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, short[] array) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, byte[] array) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, char[] array) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, double[] array) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, float[] array) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, boolean[] array) {
            setIndent(currentIndent + 1);
            super.appendDetail(buffer, fieldName, array);
            setIndent(currentIndent - 1);
        }
    }
}
