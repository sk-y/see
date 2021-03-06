/*
 * Copyright 2011 Vasily Shiyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package see.reactive;

import see.functions.VarArgFunction;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Dependency with a value, which can vary.
 * @param <T> value type
 */
public interface Signal<T> extends VarArgFunction<Void, T> {

    /**
     * Bind to signal value.
     * Used only in signal expressions.
     * @param input empty arg list
     * @return current value
     * @throws IllegalStateException if called outside signal expression
     */
    @Override
    T apply(@Nonnull List<Void> input) throws IllegalStateException;

    /**
     * Get current signal value
     * @return current value
     */
    T now();
}
