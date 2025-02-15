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
package co.elastic.apm.android.sdk.traces.session.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

import co.elastic.apm.android.sdk.traces.session.SessionIdProvider;

public class DefaultSessionIdProviderTest {

    @Test
    public void whenSessionIdIsRequested_provideNonEmptyId() {
        SessionIdProvider sessionIdProvider = getSessionIdProvider();

        String sessionId = sessionIdProvider.getSessionId();

        assertNotNull(sessionId);
        assertFalse(sessionId.isEmpty());
    }

    @Test
    public void whenSessionIdIsRequestedMultipleTimes_provideSameId() {
        SessionIdProvider sessionIdProvider = getSessionIdProvider();

        assertEquals(sessionIdProvider.getSessionId(), sessionIdProvider.getSessionId());
    }

    @Test
    public void whenSessionIdIsRequestedAgainAfter30Min_provideNewId_andKeepItWhenLessThan30MinsTimePassed() {
        int initialTime = 1_000_000;
        TestCurrentTimeMillisProvider currentTimeMillisProvider = new TestCurrentTimeMillisProvider(initialTime);
        DefaultSessionIdProvider sessionIdProvider = getSessionIdProvider(currentTimeMillisProvider);

        String firstId = sessionIdProvider.getSessionId();

        // Should change after 30 mins
        currentTimeMillisProvider.timeMillis += TimeUnit.MINUTES.toMillis(30);

        String secondId = sessionIdProvider.getSessionId();

        assertNotEquals(firstId, secondId);

        // Should keep the new id for other 30 mins

        currentTimeMillisProvider.timeMillis += TimeUnit.MINUTES.toMillis(10);

        assertEquals(secondId, sessionIdProvider.getSessionId());
    }

    @Test
    public void whenSessionIdIsRequested_timeoutShouldResetToKeepTheSameIdForOther30mins() {
        int initialTime = 1_000_000;
        TestCurrentTimeMillisProvider currentTimeMillisProvider = new TestCurrentTimeMillisProvider(initialTime);
        DefaultSessionIdProvider sessionIdProvider = getSessionIdProvider(currentTimeMillisProvider);

        String firstId = sessionIdProvider.getSessionId();

        // Forward just before 30 mins.
        currentTimeMillisProvider.timeMillis += TimeUnit.MINUTES.toMillis(29);

        assertEquals(firstId, sessionIdProvider.getSessionId());

        // Timeout should reset after the previous call to request the id.

        // Forward another 29 mins:
        currentTimeMillisProvider.timeMillis += TimeUnit.MINUTES.toMillis(29);

        assertEquals(firstId, sessionIdProvider.getSessionId());
    }

    private DefaultSessionIdProvider getSessionIdProvider() {
        return getSessionIdProvider(new SystemCurrentTimeMillisProvider());
    }

    private DefaultSessionIdProvider getSessionIdProvider(CurrentTimeMillisProvider currentTimeMillisProvider) {
        return new DefaultSessionIdProvider(currentTimeMillisProvider);
    }

    private static class TestCurrentTimeMillisProvider implements CurrentTimeMillisProvider {
        private long timeMillis;

        private TestCurrentTimeMillisProvider(long timeMillis) {
            this.timeMillis = timeMillis;
        }

        @Override
        public long getCurrentTimeMillis() {
            return timeMillis;
        }
    }
}