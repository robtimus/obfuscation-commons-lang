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

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import com.github.robtimus.obfuscation.CaseSensitivity;
import com.github.robtimus.obfuscation.Obfuscator;
import com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle;
import com.github.robtimus.obfuscation.commons.lang3.extending.CustomBuilderToStringStyle.Builder.Snapshot;

/*
 * A sub class of ObfuscatingToStringStyle that uses a custom builder. This builder will use an ObfuscatingToStringStyle.Builder Builder as delegate.
 */
@SuppressWarnings("javadoc")
public final class CustomBuilderToStringStyle extends ObfuscatingToStringStyle {

    private static final long serialVersionUID = 1L;

    private CustomBuilderToStringStyle(Builder builder) {
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
        return new Builder();
    }

    public static final class Builder {

        private final ObfuscatingToStringStyle.Builder delegate;

        // two fields copied from ToStringStyle
        private boolean useFieldNames;
        private boolean useClassName;

        private Builder() {
            // the two factories will not be used, so provide bogus methods
            delegate = new ObfuscatingToStringStyle.Builder(b -> null, s -> null);
            useFieldNames = true;
            useClassName = true;
        }

        public Builder withField(String fieldName, Obfuscator obfuscator) {
            delegate.withField(fieldName, obfuscator);
            return this;
        }

        public Builder withField(String fieldName, Obfuscator obfuscator, CaseSensitivity caseSensitivity) {
            delegate.withField(fieldName, obfuscator, caseSensitivity);
            return this;
        }

        public Builder withObfuscatedSummaries(boolean obfuscateSummaries) {
            delegate.withObfuscatedSummaries(obfuscateSummaries);
            return this;
        }

        public <R> R transform(Function<? super Builder, ? extends R> f) {
            return f.apply(this);
        }

        public Map<String, Obfuscator> obfuscators() {
            return delegate.obfuscators();
        }

        public Snapshot snapshot() {
            return new Snapshot(this);
        }

        public ObfuscatingToStringStyle build() {
            return new CustomBuilderToStringStyle(this);
        }

        public Supplier<ObfuscatingToStringStyle> supplier() {
            Snapshot snapshot = snapshot();
            return () -> new CustomBuilderToStringStyle(snapshot);
        }

        public static final class Snapshot {

            private final ObfuscatingToStringStyle.Builder.Snapshot delegate;

            private final boolean useFieldNames;
            private final boolean useClassName;

            private Snapshot(Builder builder) {
                delegate = builder.delegate.snapshot();
                useFieldNames = builder.useFieldNames;
                useClassName = builder.useClassName;
            }

            public Map<String, Obfuscator> obfuscators() {
                return delegate.obfuscators();
            }

            public boolean obfuscateSummaries() {
                return delegate.obfuscateSummaries();
            }

            public boolean useFieldNames() {
                return useFieldNames;
            }

            public boolean useClassNames() {
                return useClassName;
            }
        }
    }
}
