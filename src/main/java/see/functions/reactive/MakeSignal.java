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

package see.functions.reactive;

import com.google.common.base.Preconditions;
import com.google.common.base.Supplier;
import see.reactive.Dependency;
import see.reactive.Signal;
import see.reactive.impl.ReactiveFactory;

import java.util.Collection;
import java.util.List;

public class MakeSignal extends ReactiveFunction<Object, Signal<?>> {
    @Override
    protected Signal<?> apply(ReactiveFactory factory, final List<Object> input) {
        Preconditions.checkArgument(input.size() == 2, "Bind takes two arguments");

        Supplier<?> evaluation = new Supplier<Object>() {
            @Override
            public Object get() {
                return input.get(0);
            }
        };

        Collection<Dependency> dependencies = (Collection<Dependency>) input.get(1);

        return factory.bind(dependencies, evaluation);
    }

    @Override
    public String toString() {
        return "bind";
    }
}