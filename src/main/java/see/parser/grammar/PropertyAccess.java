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

package see.parser.grammar;

import see.util.Either;

public abstract class PropertyAccess {
    private PropertyAccess() {}

    public abstract <T, D> T accept(Visitor<T, D> visitor, D intermediate);

    /**
     * Get value contained in corresponding subclass.
     * @return property name for simple properties, evaluated index for indexed properties
     */
    public abstract Either<String, Object> value();

    /**
     * Get merged contained value - name for simple properties, index for indexed.
     * @return merged value
     */
    public abstract Object mergedValue();

    public static Simple simple(String name) {
        return new Simple(name);
    }

    public static Indexed indexed(Object index) {
        return new Indexed(index);
    }

    public static class Simple extends PropertyAccess {
        private final String name;

        private Simple(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public <T, D> T accept(Visitor<T, D> visitor, D intermediate) {
            return visitor.visit(this, intermediate);
        }

        @Override
        public Either<String, Object> value() {
            return Either.left(name);
        }

        @Override
        public Object mergedValue() {
            return name;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Simple");
            sb.append("(").append(name).append(")");
            return sb.toString();
        }
    }

    public static class Indexed extends PropertyAccess {
        private final Object index;

        private Indexed(Object index) {
            this.index = index;
        }

        public Object getIndex() {
            return index;
        }

        @Override
        public <T, D> T accept(Visitor<T, D> visitor, D intermediate) {
            return visitor.visit(this, intermediate);
        }

        @Override
        public Either<String, Object> value() {
            return Either.right(index);
        }

        @Override
        public Object mergedValue() {
            return index;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append("Indexed");
            sb.append("(").append(index).append(")");
            return sb.toString();
        }

    }

    /**
     * Visitor for PropertyAccess subclasses.
     * Supports return values and intermediate data.
     *
     * @param <T> return type
     * @param <D> intermediate data type
     */
    public static interface Visitor<T, D> {
        T visit(Simple simple, D intermediate);

        T visit(Indexed indexed, D intermediate);
    }
}
