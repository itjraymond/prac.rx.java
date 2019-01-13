package ca.jent;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.schedulers.TestScheduler;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

public class TestRxJavaSchedulerRule {

    private static final List<String> WORDS = Arrays.asList(
            "the",
            "quick",
            "brown",
            "fox",
            "jumped",
            "over",
            "the",
            "lazy",
            "dog"
    );

    private static class TestSchedulerRule implements TestRule {
        private final TestScheduler testScheduler = new TestScheduler();

        public TestScheduler getTestScheduler() {
            return testScheduler;
        }

        @Override
        public Statement apply(Statement base, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    RxJavaPlugins.setIoSchedulerHandler(scheduler -> testScheduler);
                    RxJavaPlugins.setComputationSchedulerHandler(scheduler -> testScheduler);
                    RxJavaPlugins.setNewThreadSchedulerHandler(scheduler -> testScheduler);

                    try {
                        base.evaluate();
                    } finally {
                        RxJavaPlugins.reset();
                    }
                }
            };
        }
    }

    @Rule
    public final TestSchedulerRule testSchedulerRule = new TestSchedulerRule();

    @Test
    public void testUsingTestSchedulersRule() {
        // given:
        TestObserver<String> observer = new TestObserver<>();

        Observable<String> observable = Observable
                .fromIterable(WORDS)
                .zipWith(
                        Observable.interval(1, SECONDS),
                        (str, idx) -> String.format("%2d. %s", idx, str)
                );

        observable.subscribeOn(Schedulers.computation()).subscribe(observer);

        // expect
        observer.assertNoValues();
        observer.assertNotComplete();

        // when
        testSchedulerRule.getTestScheduler().advanceTimeBy(1, SECONDS);

        // then
        observer.assertNoErrors();
        observer.assertValueCount(1);
        observer.assertValues(" 0. the");

        // when
        testSchedulerRule.getTestScheduler().advanceTimeTo(9, SECONDS);
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(9);

    }
}
