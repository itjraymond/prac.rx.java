package ca.jent;


import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.awaitility.Awaitility.await;


public class TestRxJava {

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

    @Test
    public void testInSameThread() {
        // given
        List<String> results = new ArrayList<>();
        @SuppressWarnings("unchecked")
        Observable<String> observable = Observable.fromIterable(WORDS)
                .zipWith(Observable.range(
                        1,
                        Integer.MAX_VALUE
                ), (string, index) -> String.format("%2d. %s", index, string));

        observable.subscribe(results::add);

        // then
        assertThat(results, notNullValue());
        assertThat(results, hasSize(9));
        assertThat(results, hasItem(" 4. fox"));

    }

    @Test
    public void testUsingTestSubscriber() {
        // given
        TestObserver<String> observer = new TestObserver<>();

        Observable<String> observable = Observable
                .fromIterable(WORDS)
                .zipWith(
                        Observable.range(1, Integer.MAX_VALUE),
                        (str, index) -> String.format("%2d. %s", index, str)
                );

        observable.subscribe(observer);
        // then
        observer.assertComplete();
        observer.assertNoErrors();
        observer.assertValueCount(9);

    }

    @Test
    public void testFailure() {
        // given
        TestObserver<String> observer = new TestObserver<>();
        Exception exception = new RuntimeException("boom!");

        Observable<String> observable = Observable
                .fromIterable(WORDS)
                .zipWith(
                        Observable.range(1, Integer.MAX_VALUE),
                        (str, counter) -> String.format("%2d. %s", counter, str)
                )
                .concatWith(Observable.error(exception));

        // when
        observable.subscribe(observer);

        // then
        observer.assertError(exception);
        observer.assertNotComplete();
    }

    @Test
    public void testUsingComputationScheduler() {
        //given
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable
                .fromIterable(WORDS)
                .zipWith(
                        Observable.range(1, Integer.MAX_VALUE),
                        (str,counter) -> String.format("%2d. %s", counter, str)
                );
        //when
        observable.subscribeOn(Schedulers.computation())
                  .subscribe(observer);

        await().timeout(2, SECONDS).until(observer::valueCount, equalTo(9));

        // then
        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values(), hasItem(" 4. fox"));
    }

    @Test
    public void testUsingBlockingCall() {
        // given
        Observable<String> observable = Observable
                .fromIterable(WORDS)
                .zipWith(
                        Observable.range(1, Integer.MAX_VALUE),
                        (str, counter) -> String.format("%2d. %s", counter, str)
                );

        Iterable<String> results = observable.subscribeOn(Schedulers.computation()).blockingIterable();

        // then
        assertThat(results, notNullValue());
        assertThat(results, iterableWithSize(9));
        assertThat(results, hasItem(" 4. fox"));

    }

    @Test
    public void testUsingComputationScheduler2() {
        // given:
        TestObserver<String> observer = new TestObserver<>();
        Observable<String> observable = Observable
                .fromIterable(WORDS)
                .zipWith(
                        Observable.range(1, Integer.MAX_VALUE),
                        (str, counter) -> String.format("%2d. %s", counter, str)
                );

        // when:
        observable.subscribeOn(Schedulers.computation()).subscribe(observer);

        observer.awaitTerminalEvent(2, SECONDS);

        // then:
        observer.assertComplete();
        observer.assertNoErrors();
        assertThat(observer.values(), hasItem(" 4. fox"));
    }
}
