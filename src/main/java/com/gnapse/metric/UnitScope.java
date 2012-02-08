/*
 * Copyright (C) 2012 Gnapse.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gnapse.metric;

import java.util.Collection;

/**
 * Represents a scope or domain that contains a collection of units of measure.  It can be either a
 * {@linkplain Property physical property} containing all the named units with which it can be
 * measured and expressed, or a {@linkplain Universe universe} containing all the named units of all
 * the properties that can be measured within it.
 *
 * @author Ernesto García
 */
public interface UnitScope {

    /**
     * Retrieves the unit with the given name.
     */
    Unit getUnitByName(String name);

    /**
     * Determines if this scope has a unit with the given name.
     */
    boolean hasUnitNamed(String name);

    /**
     * Returns the collection of all the units within this scope.
     */
    Collection<Unit> getNamedUnits();

}
