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

import static com.github.robtimus.obfuscation.ObfuscatorUtils.map;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.ObfuscatorUtils.MapBuilder;

/**
 * A {@link ToStringStyle} that can obfuscate fields.
 * <p>
 * An {@code ObfuscatingToStringStyle} object is not thread safe nor stateless, and should not be reused for multiple objects <i>concurrently</i>.
 * However, a {@link Builder} can build immutable {@link Supplier Suppliers} of {@code ObfuscatingToStringStyle} which can be safely reused
 * for multiple objects and in multiple threads. Such a {@link Supplier} can be used directly, or perhaps in combination with
 * {@link ThreadLocal#withInitial(Supplier)}. For instance, in a class:
 * <pre><code>
 * private static final Supplier&lt;ObfuscatingToStringStyle&gt; TO_STRING_STYLE = ObfuscatingToStringStyle.defaultStyle()
 *         ...
 *         .supplier();
 *
 * ...
 *
 * public String toString() {
 *     return ToStringBuilder.reflectionToString(this, TO_STRING_STYLE.get());
 * }
 * </code></pre>
 * <p>
 * Note: instances of {@code ObfuscatingToStringStyle} are <b>not</b> serializable, because obfuscators (in general) aren't.
 *
 * @author Rob Spoor
 */
public abstract class ObfuscatingToStringStyle extends ToStringStyle {

    private static final long serialVersionUID = 1L;

    private final Map<String, Obfuscator> obfuscators;
    private final boolean obfuscateSummaries;

    private boolean isObfuscating;

    /**
     * Creates a new obfuscating {@link ToStringStyle}.
     *
     * @param obfuscators The obfuscators to use; most often the result of calling {@link Builder#obfuscators()}.
     *                        This map is not copied but used as-is; this allows a {@link Supplier} built by a {@link Builder} to create multiple
     *                        obfuscating {@link ToStringStyle} objects with the same shared map.
     * @param obfuscateSummaries {@code true} to obfuscate summaries, or {@code false} otherwise;
     *                               most often the value set by {@link Builder#withObfuscatedSummaries(boolean)}.
     * @throws NullPointerException If the given map of obfuscators is {@code null}.
     */
    protected ObfuscatingToStringStyle(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries) {
        this.obfuscators = Objects.requireNonNull(obfuscators);
        this.obfuscateSummaries = obfuscateSummaries;

        isObfuscating = false;
    }

    final void doAppend(StringBuffer buffer, String fieldName, Consumer<StringBuffer> append) {
        if (!isObfuscating) {
            Obfuscator obfuscator = fieldName == null ? null : obfuscators.get(fieldName);
            if (obfuscator != null) {
                isObfuscating = true;
                int start = buffer.length();
                int end = start;
                try {
                    append.accept(buffer);
                    end = buffer.length();
                    obfuscator.obfuscateText(buffer, start, end, buffer);
                } finally {
                    buffer.delete(start, end);
                    isObfuscating = false;
                }
                return;
            }
        }
        // already obfuscating, or no need to obfuscate this field
        append.accept(buffer);
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
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#DEFAULT_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#DEFAULT_STYLE}.
     */
    public static Builder defaultStyle() {
        return new Builder(DefaultObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#MULTI_LINE_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#MULTI_LINE_STYLE}.
     */
    public static Builder multiLineStyle() {
        return new Builder(MultiLineObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link ToStringStyle#NO_FIELD_NAMES_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link ToStringStyle#NO_FIELD_NAMES_STYLE}.
     */
    public static Builder noFieldNamesStyle() {
        return new Builder(NoFieldNamesObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link ToStringStyle#SHORT_PREFIX_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link ToStringStyle#SHORT_PREFIX_STYLE}.
     */
    public static Builder shortPrefixStyle() {
        return new Builder(ShortPrefixObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#SIMPLE_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#SIMPLE_STYLE}.
     */
    public static Builder simpleStyle() {
        return new Builder(SimpleObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link ToStringStyle#NO_CLASS_NAME_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link ToStringStyle#NO_CLASS_NAME_STYLE}.
     */
    public static Builder noClassNameStyle() {
        return new Builder(NoClassNameObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     * This method is similar to calling {@link #recursiveStyle(Predicate)} with a predicate that always returns {@code true}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     */
    public static Builder recursiveStyle() {
        return recursiveStyle(c -> true);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     *
     * @param recurseIntoPredicate A predicate that determines which classes are recursively formatted.
     *                                 Note that primitive types, primitive wrappers and {@link String} are never recursively formatted.
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     */
    public static Builder recursiveStyle(Predicate<? super Class<?>> recurseIntoPredicate) {
        Objects.requireNonNull(recurseIntoPredicate);
        return new Builder((obfuscators, summaries) -> new RecursiveObfuscatingToStringStyle(obfuscators, summaries, recurseIntoPredicate));
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle MultilineRecursiveToStringStyle}.
     * This method is similar to calling {@link #recursiveStyle(Predicate)} with a predicate that always returns {@code true}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     */
    public static Builder multiLineRecursiveStyle() {
        return multiLineRecursiveStyle(c -> true);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link org.apache.commons.lang3.builder.MultilineRecursiveToStringStyle MultilineRecursiveToStringStyle}.
     *
     * @param recurseIntoPredicate A predicate that determines which classes are recursively formatted.
     *                                 Note that primitive types, primitive wrappers and {@link String} are never recursively formatted.
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link org.apache.commons.lang3.builder.RecursiveToStringStyle RecursiveToStringStyle}.
     */
    public static Builder multiLineRecursiveStyle(Predicate<? super Class<?>> recurseIntoPredicate) {
        Objects.requireNonNull(recurseIntoPredicate);
        return new Builder((obfuscators, summaries) -> new MultiLineRecursiveObfuscatingToStringStyle(obfuscators, summaries, recurseIntoPredicate));
    }

    /**
     * A factory for creating {@link ObfuscatingToStringStyle ObfuscatingToStringStyles}.
     * <p>
     * In most cases, a constructor of a sub class of {@link ObfuscatingToStringStyle} that delegates to
     * {@link ObfuscatingToStringStyle#ObfuscatingToStringStyle(Map, boolean)} is used for this factory.
     *
     * @author Rob Spoor
     * @param <T> The type of {@link ObfuscatingToStringStyle} to create.
     */
    public interface Factory<T extends ObfuscatingToStringStyle> {

        /**
         * Creates a new {@link ObfuscatingToStringStyle} object.
         *
         * @param obfuscators The obfuscators to use.
         * @param obfuscateSummaries {@code true} to obfuscate summaries, or {@code false} otherwise.
         * @return A new {@link ObfuscatingToStringStyle} object.
         */
        T create(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries);
    }

    /**
     * A builder for creating obfuscating {@link ToStringStyle} objects.
     * In addition, it can create {@link Supplier Suppliers} of obfuscating {@link ToStringStyle} objects. These can be used as a more light-weight
     * way of creating obfuscating {@link ToStringStyle} objects; whereas creating obfuscating {@link ToStringStyle} objects using {@link #build()}
     * will always call {@link #obfuscators()}, {@link Supplier Suppliers} created using {@link #supplier()} will create obfuscating
     * {@link ToStringStyle} objects from a shared copy instead. You should use {@link #supplier()} instead of {@link #build()} if you plan on
     * creating multiple obfuscating {@link ToStringStyle} objects with the same settings.
     *
     * @author Rob Spoor
     */
    public static final class Builder {

        private final Factory<?> factory;

        private final MapBuilder<Obfuscator> obfuscators;

        private boolean obfuscateSummaries = false;

        /**
         * Creates a new builder.
         * <p>
         * This builder will use a factory to build {@link Supplier Suppliers} of {@link ObfuscatingToStringStyle ObfuscatingToStringStyles} based on
         * this builder. This allows the building of {@link Supplier Suppliers} of sub classes of {@link ObfuscatingToStringStyle}; just create a
         * constructor that delegates to {@link ObfuscatingToStringStyle#ObfuscatingToStringStyle(Map, boolean)}, and provide this constructor
         * as factory to this constructor.
         *
         * @param factory The factory to build {@link ObfuscatingToStringStyle ObfuscatingToStringStyles} based on this builder.
         * @throws NullPointerException If the given factory is {@code null}.
         */
        public Builder(Factory<?> factory) {
            this.factory = Objects.requireNonNull(factory);

            obfuscators = map();
        }

        /**
         * Adds a field to obfuscate.
         * This method is an alias for {@link #withField(String, Obfuscator, boolean) withField(fieldName, obfuscator, true)}.
         *
         * @param fieldName The name of the field. It will be treated case sensitively.
         * @param obfuscator The obfuscator to use for obfuscating the field.
         * @return This object.
         * @throws NullPointerException If the given field name or obfuscator is {@code null}.
         */
        public Builder withField(String fieldName, Obfuscator obfuscator) {
            return withField(fieldName, obfuscator, true);
        }

        /**
         * Adds a field to obfuscate.
         *
         * @param fieldName The name of the field.
         * @param obfuscator The obfuscator to use for obfuscating the field.
         * @param caseSensitive {@code true} if the field name should be treated case sensitively,
         *                          or {@code false} if it should be treated case insensitively.
         * @return This object.
         * @throws NullPointerException If the given field name or obfuscator is {@code null}.
         */
        public Builder withField(String fieldName, Obfuscator obfuscator, boolean caseSensitive) {
            obfuscators.withEntry(fieldName, obfuscator, caseSensitive);
            return this;
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

        /**
         * This method allows the application of a function to this builder.
         * <p>
         * Any exception thrown by the function will be propagated to the caller.
         *
         * @param <R> The type of the result of the function.
         * @param f The function to apply.
         * @return The result of applying the function to this builder.
         */
        public <R> R transform(Function<? super Builder, ? extends R> f) {
            return f.apply(this);
        }

        /**
         * Returns a new {@code StringMap} with all added obfuscators.
         *
         * @return A new {@code StringMap} with all added obfuscators.
         */
        public Map<String, Obfuscator> obfuscators() {
            return obfuscators.build();
        }

        /**
         * Creates a new obfuscating {@link ToStringStyle} with the fields and obfuscators added to this builder.
         *
         * @return The created obfuscating {@link ToStringStyle}.
         */
        public ObfuscatingToStringStyle build() {
            return factory.create(obfuscators(), obfuscateSummaries);
        }

        /**
         * Creates a new {@link Supplier} that will create obfuscating {@link ToStringStyle} objects with the properties and obfuscators added to this
         * builder. Unlike {@link #build()}, this method will create a snapshot of the current settings of this builder and reuses those to create
         * obfuscating {@link ToStringStyle} objects. This makes this method more light-weight when you need to create multiple obfuscating
         * {@link ToStringStyle} objects with the current settings of this builder.
         *
         * @return A new {@link Supplier} that will create obfuscating {@link ToStringStyle} objects with the properties and obfuscators added to this
         *         builder
         */
        public Supplier<ObfuscatingToStringStyle> supplier() {
            // capture the current settings
            Map<String, Obfuscator> currentObfuscators = obfuscators();
            boolean currentObfuscateSummaries = obfuscateSummaries;

            return () -> factory.create(currentObfuscators, currentObfuscateSummaries);
        }
    }

    private static final class DefaultObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private DefaultObfuscatingToStringStyle(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries) {
            super(obfuscators, obfuscateSummaries);
            // no modifications
        }
    }

    private static final class MultiLineObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private MultiLineObfuscatingToStringStyle(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries) {
            super(obfuscators, obfuscateSummaries);
            setContentStart("["); //$NON-NLS-1$
            setFieldSeparator(System.lineSeparator() + "  "); //$NON-NLS-1$
            setFieldSeparatorAtStart(true);
            setContentEnd(System.lineSeparator() + "]"); //$NON-NLS-1$
        }
    }

    private static final class NoFieldNamesObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private NoFieldNamesObfuscatingToStringStyle(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries) {
            super(obfuscators, obfuscateSummaries);
            setUseFieldNames(false);
        }
    }

    private static final class ShortPrefixObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private ShortPrefixObfuscatingToStringStyle(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries) {
            super(obfuscators, obfuscateSummaries);
            setUseShortClassName(true);
            setUseIdentityHashCode(false);
        }
    }

    private static final class SimpleObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private SimpleObfuscatingToStringStyle(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries) {
            super(obfuscators, obfuscateSummaries);
            setUseClassName(false);
            setUseIdentityHashCode(false);
            setUseFieldNames(false);
            setContentStart(""); //$NON-NLS-1$
            setContentEnd(""); //$NON-NLS-1$
        }
    }

    private static final class NoClassNameObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private NoClassNameObfuscatingToStringStyle(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries) {
            super(obfuscators, obfuscateSummaries);
            setUseClassName(false);
            setUseIdentityHashCode(false);
        }
    }

    private static class RecursiveObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private final Predicate<? super Class<?>> recurseIntoPredicate;

        RecursiveObfuscatingToStringStyle(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries,
                Predicate<? super Class<?>> recurseIntoPredicate) {

            super(obfuscators, obfuscateSummaries);

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

        private MultiLineRecursiveObfuscatingToStringStyle(Map<String, Obfuscator> obfuscators, boolean obfuscateSummaries,
                Predicate<? super Class<?>> recurseIntoPredicate) {

            super(obfuscators, obfuscateSummaries, recurseIntoPredicate);

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

        private void increaseIndent() {
            setIndent(currentIndent + 1);
        }

        private void decreaseIndent() {
            setIndent(currentIndent - 1);
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
                increaseIndent();
                try {
                    doAppend(buffer, fieldName, b -> {
                        // ignore the result of toString(), since the new ReflectionToStringBuilder will share the buffer and append directly to it
                        new ReflectionToStringBuilder(value, this, b).toString();
                    });
                } finally {
                    decreaseIndent();
                }
            } else {
                super.appendDetail(buffer, fieldName, value);
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, Collection<?> coll) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, coll);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, Map<?, ?> map) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, map);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, Object[] array) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void reflectionAppendArrayDetail(StringBuffer buffer, String fieldName, Object array) {
            increaseIndent();
            try {
                super.reflectionAppendArrayDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, long[] array) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, int[] array) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, short[] array) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, byte[] array) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, char[] array) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, double[] array) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, float[] array) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }

        @Override
        protected void appendDetail(StringBuffer buffer, String fieldName, boolean[] array) {
            increaseIndent();
            try {
                super.appendDetail(buffer, fieldName, array);
            } finally {
                decreaseIndent();
            }
        }
    }
}
