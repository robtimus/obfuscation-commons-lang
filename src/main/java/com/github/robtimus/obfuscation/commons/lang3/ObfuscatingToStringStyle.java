/*
 * ObfuscatingToStringStyle.java
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

import static com.github.robtimus.obfuscation.support.CaseSensitivity.CASE_SENSITIVE;
import static com.github.robtimus.obfuscation.support.ObfuscatorUtils.map;
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
import com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.Builder.Snapshot;
import com.github.robtimus.obfuscation.support.CaseSensitivity;
import com.github.robtimus.obfuscation.support.ObfuscatorUtils.MapBuilder;

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
 * Note: instances of {@code ObfuscatingToStringStyle} are usually <b>not</b> serializable, because obfuscators (in general) aren't.
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
     * @param builder The builder with the settings to use for creating the obfuscating {@link ToStringStyle}.
     * @throws NullPointerException If the given builder is {@code null}.
     */
    protected ObfuscatingToStringStyle(Builder builder) {
        obfuscators = builder.obfuscators();
        obfuscateSummaries = builder.obfuscateSummaries;

        isObfuscating = false;
    }

    /**
     * Creates a new obfuscating {@link ToStringStyle}.
     *
     * @param snapshot A builder snapshot with the settings to use for creating the obfuscating {@link ToStringStyle}.
     * @throws NullPointerException If the given snapshot is {@code null}.
     */
    protected ObfuscatingToStringStyle(Snapshot snapshot) {
        obfuscators = snapshot.obfuscators;
        obfuscateSummaries = snapshot.obfuscateSummaries;

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
        return new Builder(DefaultObfuscatingToStringStyle::new, DefaultObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#MULTI_LINE_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#MULTI_LINE_STYLE}.
     */
    public static Builder multiLineStyle() {
        return new Builder(MultiLineObfuscatingToStringStyle::new, MultiLineObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link ToStringStyle#NO_FIELD_NAMES_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link ToStringStyle#NO_FIELD_NAMES_STYLE}.
     */
    public static Builder noFieldNamesStyle() {
        return new Builder(NoFieldNamesObfuscatingToStringStyle::new, NoFieldNamesObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link ToStringStyle#SHORT_PREFIX_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link ToStringStyle#SHORT_PREFIX_STYLE}.
     */
    public static Builder shortPrefixStyle() {
        return new Builder(ShortPrefixObfuscatingToStringStyle::new, ShortPrefixObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#SIMPLE_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to {@link ToStringStyle#SIMPLE_STYLE}.
     */
    public static Builder simpleStyle() {
        return new Builder(SimpleObfuscatingToStringStyle::new, SimpleObfuscatingToStringStyle::new);
    }

    /**
     * Returns a builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     * {@link ToStringStyle#NO_CLASS_NAME_STYLE}.
     *
     * @return A builder that creates obfuscating {@link ToStringStyle} objects that produce output similar to
     *         {@link ToStringStyle#NO_CLASS_NAME_STYLE}.
     */
    public static Builder noClassNameStyle() {
        return new Builder(NoClassNameObfuscatingToStringStyle::new, NoClassNameObfuscatingToStringStyle::new);
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
        return new Builder(builder -> new RecursiveObfuscatingToStringStyle(builder, recurseIntoPredicate),
                snapshot -> new RecursiveObfuscatingToStringStyle(snapshot, recurseIntoPredicate));
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
        return new Builder(builder -> new MultiLineRecursiveObfuscatingToStringStyle(builder, recurseIntoPredicate),
                snapshot -> new MultiLineRecursiveObfuscatingToStringStyle(snapshot, recurseIntoPredicate));
    }

    /**
     * A builder for creating obfuscating {@link ToStringStyle} objects.
     * <p>
     * In addition, it can create {@link Supplier Suppliers} of obfuscating {@link ToStringStyle} objects. These can be used as a more light-weight
     * way of creating obfuscating {@link ToStringStyle} objects; whereas creating obfuscating {@link ToStringStyle} objects using {@link #build()}
     * will always call {@link #obfuscators()}, {@link Supplier Suppliers} created using {@link #supplier()} will create obfuscating
     * {@link ToStringStyle} objects from a shared copy instead. You should use {@link #supplier()} instead of {@link #build()} if you plan on
     * creating multiple obfuscating {@link ToStringStyle} objects with the same settings.
     *
     * @author Rob Spoor
     */
    public static final class Builder {

        private final Function<? super Builder, ? extends ObfuscatingToStringStyle> fromBuilderConstructor;
        private final Function<? super Snapshot, ? extends ObfuscatingToStringStyle> fromSnapshotConstructor;

        private final MapBuilder<Obfuscator> obfuscators;

        private boolean obfuscateSummaries = false;

        /**
         * Creates a new builder.
         * <p>
         * This builder will use two factories to build {@link ObfuscatingToStringStyle} and {@link Supplier Suppliers} of
         * {@link ObfuscatingToStringStyle ObfuscatingToStringStyles} based on the settings of this builder. This allows the building of sub classes
         * of {@link ObfuscatingToStringStyle} and {@code Suppliers} of such sub classes; just create two constructors that delegate to
         * {@link ObfuscatingToStringStyle#ObfuscatingToStringStyle(Builder)} and
         * {@link ObfuscatingToStringStyle#ObfuscatingToStringStyle(ObfuscatingToStringStyle.Builder.Snapshot)} respectively, and provide these
         * constructors as factories to this {@code Builder} constructor.
         * <p>
         * For example:
         * <pre><code>
         * public final class MyToStringStyle extends ObfuscatingToStringStyle {
         *
         *     private MyToStringStyle(Builder builder) {
         *         super(builder);
         *         // custom configuration
         *     }
         *
         *     private MyToStringStyle(Snapshot snapshot) {
         *         super(snapshot);
         *         // custom configuration
         *     }
         *
         *     public static Builder builder() {
         *         return new Builder(MyToStringStyle::new, MyToStringStyle::new);
         *     }
         * }
         * </code></pre>
         *
         * @param fromBuilderConstructor The factory to build {@link ObfuscatingToStringStyle ObfuscatingToStringStyles} based on the current settings
         *                                   of this builder.
         * @param fromSnapshotConstructor The factory to build {@link ObfuscatingToStringStyle ObfuscatingToStringStyles} based on a snapshot of this
         *                                    builder.
         * @throws NullPointerException If either of the given factories is {@code null}.
         */
        public Builder(Function<? super Builder, ? extends ObfuscatingToStringStyle> fromBuilderConstructor,
                Function<? super Snapshot, ? extends ObfuscatingToStringStyle> fromSnapshotConstructor) {

            this.fromBuilderConstructor = Objects.requireNonNull(fromBuilderConstructor);
            this.fromSnapshotConstructor = Objects.requireNonNull(fromSnapshotConstructor);

            obfuscators = map();
        }

        /**
         * Adds a field to obfuscate.
         * This method is an alias for {@link #withField(String, Obfuscator, CaseSensitivity) withField(fieldName, obfuscator, CASE_SENSITIVE)}.
         *
         * @param fieldName The name of the field. It will be treated case sensitively.
         * @param obfuscator The obfuscator to use for obfuscating the field.
         * @return This object.
         * @throws NullPointerException If the given field name or obfuscator is {@code null}.
         * @throws IllegalArgumentException If a field with the same name and the same case sensitivity was already added.
         */
        public Builder withField(String fieldName, Obfuscator obfuscator) {
            return withField(fieldName, obfuscator, CASE_SENSITIVE);
        }

        /**
         * Adds a field to obfuscate.
         *
         * @param fieldName The name of the field.
         * @param obfuscator The obfuscator to use for obfuscating the field.
         * @param caseSensitivity The case sensitivity for the key.
         * @return This object.
         * @throws NullPointerException If the given field name, obfuscator or case sensitivity is {@code null}.
         * @throws IllegalArgumentException If a field with the same name and the same case sensitivity was already added.
         */
        public Builder withField(String fieldName, Obfuscator obfuscator, CaseSensitivity caseSensitivity) {
            obfuscators.withEntry(fieldName, obfuscator, caseSensitivity);
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
         * Creates a new immutable map with all added obfuscators.
         *
         * @return The created map.
         */
        public Map<String, Obfuscator> obfuscators() {
            return obfuscators.build();
        }

        /**
         * Creates a new snapshot of this builder.
         *
         * @return The created snapshot.
         */
        public Snapshot snapshot() {
            return new Snapshot(this);
        }

        /**
         * Creates a new obfuscating {@link ToStringStyle} with the fields and obfuscators added to this builder.
         *
         * @return The created obfuscating {@link ToStringStyle}.
         */
        public ObfuscatingToStringStyle build() {
            return fromBuilderConstructor.apply(this);
        }

        /**
         * Creates a new {@link Supplier} that will create obfuscating {@link ToStringStyle} objects with the properties and obfuscators added to this
         * builder.
         * <p>
         * Unlike {@link #build()}, this method will create a {@link #snapshot() snapshot} of the current settings of this builder and reuses those to
         * create obfuscating {@link ToStringStyle} objects. This makes this method more light-weight when you need to create multiple obfuscating
         * {@link ToStringStyle} objects with the current settings of this builder.
         *
         * @return The created {@link Supplier}.
         */
        public Supplier<ObfuscatingToStringStyle> supplier() {
            Snapshot snapshot = snapshot();
            return () -> fromSnapshotConstructor.apply(snapshot);
        }

        /**
         * A snapshot of the settings of a {@link Builder}. This can be used to create multiple obfuscating {@link ToStringStyle} objects with the
         * same settings.
         *
         * @author Rob Spoor
         */
        public static final class Snapshot {

            private final Map<String, Obfuscator> obfuscators;

            private boolean obfuscateSummaries = false;

            private Snapshot(Builder builder) {
                obfuscators = builder.obfuscators();
                obfuscateSummaries = builder.obfuscateSummaries;
            }

            /**
             * Returns an unmodifiable map with all obfuscators.
             *
             * @return An unmodifiable map with all obfuscators.
             */
            public Map<String, Obfuscator> obfuscators() {
                return obfuscators;
            }

            /**
             * Returns whether or not to obfuscate summaries as well as details.
             *
             * @return {@code true} to obfuscate summaries and details, or {@code false} to only obfusate details.
             */
            public boolean obfuscateSummaries() {
                return obfuscateSummaries;
            }
        }
    }

    private static final class DefaultObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private DefaultObfuscatingToStringStyle(Builder builder) {
            super(builder);
            // no modifications
        }

        private DefaultObfuscatingToStringStyle(Snapshot snapshot) {
            super(snapshot);
            // no modifications
        }
    }

    private static final class MultiLineObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private MultiLineObfuscatingToStringStyle(Builder builder) {
            super(builder);
            configure();
        }

        private MultiLineObfuscatingToStringStyle(Snapshot snapshot) {
            super(snapshot);
            configure();
        }

        private void configure() {
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
            configure();
        }

        private NoFieldNamesObfuscatingToStringStyle(Snapshot snapshot) {
            super(snapshot);
            configure();
        }

        private void configure() {
            setUseFieldNames(false);
        }
    }

    private static final class ShortPrefixObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private ShortPrefixObfuscatingToStringStyle(Builder builder) {
            super(builder);
            configure();
        }

        private ShortPrefixObfuscatingToStringStyle(Snapshot snapshot) {
            super(snapshot);
            configure();
        }

        private void configure() {
            setUseShortClassName(true);
            setUseIdentityHashCode(false);
        }
    }

    private static final class SimpleObfuscatingToStringStyle extends ObfuscatingToStringStyle {

        private static final long serialVersionUID = 1L;

        private SimpleObfuscatingToStringStyle(Builder builder) {
            super(builder);
            configure();
        }

        private SimpleObfuscatingToStringStyle(Snapshot snapshot) {
            super(snapshot);
            configure();
        }

        private void configure() {
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
            configure();
        }

        private NoClassNameObfuscatingToStringStyle(Snapshot snapshot) {
            super(snapshot);
            configure();
        }

        private void configure() {
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

        RecursiveObfuscatingToStringStyle(Snapshot snapshot, Predicate<? super Class<?>> recurseIntoPredicate) {
            super(snapshot);
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

        private MultiLineRecursiveObfuscatingToStringStyle(Snapshot snapshot, Predicate<? super Class<?>> recurseIntoPredicate) {
            super(snapshot, recurseIntoPredicate);
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
