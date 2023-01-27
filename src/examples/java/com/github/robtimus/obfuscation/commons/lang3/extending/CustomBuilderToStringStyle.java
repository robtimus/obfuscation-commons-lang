/*
 * CustomBuilderToStringStyle.java
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

package com.github.robtimus.obfuscation.commons.lang3.extending;

import java.util.function.Function;
import java.util.function.Supplier;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle;
import com.github.robtimus.obfuscation.commons.lang3.extending.CustomBuilderToStringStyle.Builder.Snapshot;
import com.github.robtimus.obfuscation.support.CaseSensitivity;

/*
 * A sub class of ObfuscatingToStringStyle that uses a custom builder. This builder will use an ObfuscatingToStringStyle.Builder Builder as delegate.
 */
final class CustomBuilderToStringStyle extends ObfuscatingToStringStyle {

    private static final long serialVersionUID = 1L;

    private CustomBuilderToStringStyle(ToStringStyleBuilder builder) {
        super(builder.delegate);
        setUseFieldNames(builder.useFieldNames);
        setUseClassName(builder.useClassName);
    }

    private CustomBuilderToStringStyle(Snapshot snapshot) {
        super(snapshot.delegate);
        setUseFieldNames(snapshot.useFieldNames);
        setUseClassName(snapshot.useClassName);
    }

    public static Builder builder() {
        return new ToStringStyleBuilder();
    }

    public abstract static class Builder {

        private Builder() {
        }

        public abstract FieldConfigurer withField(String fieldName, Obfuscator obfuscator);

        public abstract FieldConfigurer withField(String fieldName, Obfuscator obfuscator, CaseSensitivity caseSensitivity);

        public abstract Builder caseSensitiveByDefault();

        public abstract Builder caseInsensitiveByDefault();

        public abstract Builder includeSummariesByDefault();

        public abstract Builder excludeSummariesByDefault();

        public abstract Builder useFieldNames(boolean useFieldNames);

        public abstract Builder useClassName(boolean useClassName);

        public <R> R transform(Function<? super Builder, ? extends R> f) {
            return f.apply(this);
        }

        public abstract Snapshot snapshot();

        public abstract ObfuscatingToStringStyle build();

        public Supplier<ObfuscatingToStringStyle> supplier() {
            Snapshot snapshot = snapshot();
            return snapshot::build;
        }

        public static final class Snapshot {

            private final ObfuscatingToStringStyle.Builder.Snapshot delegate;

            private final boolean useFieldNames;
            private final boolean useClassName;

            private Snapshot(ToStringStyleBuilder builder) {
                delegate = builder.delegate.snapshot();
                useFieldNames = builder.useFieldNames;
                useClassName = builder.useClassName;
            }

            public ObfuscatingToStringStyle build() {
                return new CustomBuilderToStringStyle(this);
            }
        }
    }

    public abstract static class FieldConfigurer extends Builder {

        private FieldConfigurer() {
            super();
        }

        public abstract FieldConfigurer includeSummaries();

        public abstract FieldConfigurer excludeSummaries();
    }

    public static final class ToStringStyleBuilder extends FieldConfigurer {

        private final ObfuscatingToStringStyle.Builder delegate;

        private ObfuscatingToStringStyle.FieldConfigurer fieldDelegate;

        // two fields copied from ToStringStyle
        private boolean useFieldNames;
        private boolean useClassName;

        private ToStringStyleBuilder() {
            // the two factories will not be used, so provide bogus methods
            delegate = ObfuscatingToStringStyle.Builder.create(b -> null, s -> null);
            useFieldNames = true;
            useClassName = true;
        }

        @Override
        public FieldConfigurer withField(String fieldName, Obfuscator obfuscator) {
            fieldDelegate = delegate.withField(fieldName, obfuscator);
            return this;
        }

        @Override
        public FieldConfigurer withField(String fieldName, Obfuscator obfuscator, CaseSensitivity caseSensitivity) {
            fieldDelegate = delegate.withField(fieldName, obfuscator, caseSensitivity);
            return this;
        }

        @Override
        public Builder caseSensitiveByDefault() {
            delegate.caseSensitiveByDefault();
            return this;
        }

        @Override
        public Builder caseInsensitiveByDefault() {
            delegate.caseInsensitiveByDefault();
            return this;
        }

        @Override
        public Builder includeSummariesByDefault() {
            delegate.includeSummariesByDefault();
            return this;
        }

        @Override
        public Builder excludeSummariesByDefault() {
            delegate.excludeSummariesByDefault();
            return this;
        }

        @Override
        public FieldConfigurer includeSummaries() {
            fieldDelegate.includeSummaries();
            return this;
        }

        @Override
        public FieldConfigurer excludeSummaries() {
            fieldDelegate.excludeSummaries();
            return this;
        }

        @Override
        public Builder useFieldNames(boolean useFieldNames) {
            this.useFieldNames = useFieldNames;
            return this;
        }

        @Override
        public Builder useClassName(boolean useClassName) {
            this.useClassName = useClassName;
            return this;
        }

        @Override
        public <R> R transform(Function<? super Builder, ? extends R> f) {
            return f.apply(this);
        }

        @Override
        public Snapshot snapshot() {
            return new Snapshot(this);
        }

        @Override
        public ObfuscatingToStringStyle build() {
            return new CustomBuilderToStringStyle(this);
        }
    }
}
