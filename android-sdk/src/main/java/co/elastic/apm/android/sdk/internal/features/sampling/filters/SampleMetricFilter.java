/*
 * Licensed to Elasticsearch B.V. under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch B.V. licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package co.elastic.apm.android.sdk.internal.features.sampling.filters;

import co.elastic.apm.android.sdk.internal.features.sampling.SamplingPolicy;
import co.elastic.apm.android.sdk.metrics.tools.MetricFilter;
import io.opentelemetry.sdk.metrics.data.MetricData;

public final class SampleMetricFilter implements MetricFilter {
    private final SamplingPolicy policy;

    public SampleMetricFilter(SamplingPolicy policy) {
        this.policy = policy;
    }

    @Override
    public boolean shouldInclude(MetricData item) {
        return policy.allowSignalExporting();
    }
}
