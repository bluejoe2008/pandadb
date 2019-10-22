/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
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
package org.neo4j.driver.internal.metrics;

import java.util.HashMap;
import java.util.Map;

import org.neo4j.driver.ConnectionPoolMetrics;
import org.neo4j.driver.Metrics;

public class SnapshotMetrics implements Metrics
{
    private final Map<String,ConnectionPoolMetrics> poolMetrics;

    public SnapshotMetrics( Metrics metrics )
    {
        Map<String,ConnectionPoolMetrics> other = metrics.connectionPoolMetrics();
        poolMetrics = new HashMap<>( other.size() );

        for ( String id : other.keySet() )
        {
            poolMetrics.put( id, other.get( id ).snapshot() );
        }
    }

    @Override
    public Map<String,ConnectionPoolMetrics> connectionPoolMetrics()
    {
        return poolMetrics;
    }

    @Override
    public Metrics snapshot()
    {
        return this;
    }
}