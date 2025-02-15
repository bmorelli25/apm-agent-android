package co.elastic.apm.android.test;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import co.elastic.apm.android.test.activities.CoroutineActivity;
import co.elastic.apm.android.test.base.BaseEspressoTest;
import co.elastic.apm.android.test.common.spans.Spans;
import io.opentelemetry.sdk.trace.data.SpanData;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class CoroutineInstrumentationTest extends BaseEspressoTest<CoroutineActivity> {

    @Test
    public void onCreation_whenCoroutineGetsLaunched_propagateCurrentSpanContext() {
        List<SpanData> spans = getRecordedSpans(3);

        SpanData rootSpan = spans.get(0);
        SpanData onCreateSpan = spans.get(1);
        SpanData myCoroutineSpan = spans.get(2);

        Spans.verify(onCreateSpan)
                .isDirectChildOf(rootSpan);

        Spans.verify(myCoroutineSpan)
                .isNamed("My span inside a coroutine")
                .isDirectChildOf(onCreateSpan);
    }

    @Override
    protected Class<CoroutineActivity> getActivityClass() {
        return CoroutineActivity.class;
    }
}
