/*
 * ReusingBuilderToStringStyle.java
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

import com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle;
import com.github.robtimus.obfuscation.commons.lang3.ObfuscatingToStringStyle.Builder.Snapshot;

/*
 * A sub class of ObfuscatingToStringStyle that reuses ObfuscatingToStringStyle.Builder directly.
 */
final class ReusingBuilderToStringStyle extends ObfuscatingToStringStyle {

    private static final long serialVersionUID = 1L;

    private ReusingBuilderToStringStyle(Builder builder) {
        super(builder);
        configure();
    }

    private ReusingBuilderToStringStyle(Snapshot snapshot) {
        super(snapshot);
        configure();
    }

    private void configure() {
        // methods of ToStringStyle can be called here to configure the instance
    }

    public static Builder builder() {
        return Builder.create(ReusingBuilderToStringStyle::new, ReusingBuilderToStringStyle::new);
    }
}
