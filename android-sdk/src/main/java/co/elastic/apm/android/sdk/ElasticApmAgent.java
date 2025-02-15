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
package co.elastic.apm.android.sdk;

import android.content.Context;

import androidx.annotation.NonNull;

import co.elastic.apm.android.common.internal.logging.Elog;
import co.elastic.apm.android.sdk.attributes.AttributesCompose;
import co.elastic.apm.android.sdk.internal.logging.AndroidLoggerFactory;
import co.elastic.apm.android.sdk.internal.services.Service;
import co.elastic.apm.android.sdk.internal.services.ServiceManager;
import co.elastic.apm.android.sdk.internal.services.metadata.ApmMetadataService;
import co.elastic.apm.android.sdk.internal.services.network.NetworkService;
import co.elastic.apm.android.sdk.internal.services.permissions.AndroidPermissionService;
import co.elastic.apm.android.sdk.providers.Provider;
import co.elastic.apm.android.sdk.providers.SimpleProvider;
import co.elastic.apm.android.sdk.traces.connectivity.Connectivity;
import co.elastic.apm.android.sdk.traces.otel.processor.ElasticSpanProcessor;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;

public final class ElasticApmAgent {

    public final ElasticApmConfiguration configuration;
    private static ElasticApmAgent instance;
    private final Provider<Connectivity> connectivityProvider;
    private final ServiceManager serviceManager;
    private final AttributesCompose globalAttributes;

    public static ElasticApmAgent get() {
        verifyInitialization();
        return instance;
    }

    public static ElasticApmAgent initialize(Context context) {
        return initialize(context, null, null);
    }

    public static ElasticApmAgent initialize(Context context, ElasticApmConfiguration configuration) {
        return initialize(context, configuration, null);
    }

    public static ElasticApmAgent initialize(Context context, Connectivity connectivity) {
        return initialize(context, null, connectivity);
    }

    public synchronized static ElasticApmAgent initialize(Context context, ElasticApmConfiguration configuration, Connectivity connectivity) {
        if (instance != null) {
            throw new IllegalStateException("Already initialized");
        }
        Elog.init(new AndroidLoggerFactory());
        Provider<Connectivity> connectivityProvider;
        if (connectivity != null) {
            connectivityProvider = new SimpleProvider<>(connectivity);
        } else {
            connectivityProvider = Connectivity.getDefault();
        }
        ElasticApmConfiguration apmConfiguration;
        if (configuration != null) {
            apmConfiguration = configuration;
        } else {
            apmConfiguration = ElasticApmConfiguration.getDefault();
        }
        instance = new ElasticApmAgent(context, connectivityProvider, apmConfiguration);
        instance.onInitializationFinished();
        return instance;
    }

    private static void verifyInitialization() {
        if (instance == null) {
            throw new IllegalStateException("ElasticApmAgent hasn't been initialized");
        }
    }

    public void destroy() {
        serviceManager.stop();
        instance = null;
    }

    public <T extends Service> T getService(String name) {
        return serviceManager.getService(name);
    }

    ElasticApmAgent(Context context, Provider<Connectivity> connectivityProvider, ElasticApmConfiguration configuration) {
        Context appContext = context.getApplicationContext();
        this.connectivityProvider = connectivityProvider;
        this.configuration = configuration;
        serviceManager = new ServiceManager();
        serviceManager.addService(new NetworkService(appContext));
        serviceManager.addService(new AndroidPermissionService(appContext));
        serviceManager.addService(new ApmMetadataService(appContext));
        globalAttributes = AttributesCompose.global(appContext, configuration.serviceName, configuration.serviceVersion);
    }

    private void onInitializationFinished() {
        serviceManager.start();
        initializeOpentelemetry();
    }

    private void initializeOpentelemetry() {
        OpenTelemetrySdk.builder()
                .setTracerProvider(getTracerProvider())
                .setPropagators(getContextPropagator())
                .buildAndRegisterGlobal();
    }

    private SdkTracerProvider getTracerProvider() {
        Resource resource = Resource.getDefault()
                .merge(globalAttributes.provideAsResource());

        ElasticSpanProcessor processor = getProcessor();
        processor.addAllExclusionRules(configuration.httpTraceConfiguration.exclusionRules);

        return SdkTracerProvider.builder()
                .addSpanProcessor(processor)
                .setResource(resource)
                .build();
    }

    @NonNull
    private ElasticSpanProcessor getProcessor() {
        SpanProcessor spanProcessor = connectivityProvider.get().getSpanProcessor();
        if (spanProcessor instanceof ElasticSpanProcessor) {
            return (ElasticSpanProcessor) spanProcessor;
        }
        return new ElasticSpanProcessor(spanProcessor);
    }

    private ContextPropagators getContextPropagator() {
        return ContextPropagators.create(W3CTraceContextPropagator.getInstance());
    }
}