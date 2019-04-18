package com.rxjava.rxlife;

import android.arch.lifecycle.Lifecycle.Event;
import android.arch.lifecycle.LifecycleOwner;

import org.reactivestreams.Subscriber;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.parallel.ParallelFlowable;

/**
 * User: ljx
 * Date: 2019/4/18
 * Time: 18:40
 */
public class ParallelFlowableLife<T> {

    private ParallelFlowable<T> upStream;
    private LifecycleOwner      owner;
    private Event               event;
    private boolean             onMain;

    ParallelFlowableLife(ParallelFlowable<T> upStream, LifecycleOwner owner, Event event, boolean onMain) {
        this.upStream = upStream;
        this.owner = owner;
        this.event = event;
        this.onMain = onMain;
    }

    @SuppressWarnings("unchecked")
    public void subscribe(@NonNull Subscriber<? super T>[] subscribers) {
        if (!validate(subscribers)) {
            return;
        }

        int n = subscribers.length;

        Subscriber<? super T>[] parents = new Subscriber[n];

        for (int i = 0; i < n; i++) {
            Subscriber<? super T> a = subscribers[i];
            if (a instanceof ConditionalSubscriber) {
                parents[i] = new LifeConditionalSubscriber<>((ConditionalSubscriber<? super T>) a, owner, event);
            } else {
                parents[i] = new LifeSubscriber<>(a, owner, event);
            }
        }
        ParallelFlowable<T> upStream = this.upStream;
        if (onMain) upStream = upStream.runOn(AndroidSchedulers.mainThread());
        upStream.subscribe(parents);
    }

    private int parallelism() {
        return upStream.parallelism();
    }

    private boolean validate(@NonNull Subscriber<?>[] subscribers) {
        int p = parallelism();
        if (subscribers.length != p) {
            Throwable iae = new IllegalArgumentException("parallelism = " + p + ", subscribers = " + subscribers.length);
            for (Subscriber<?> s : subscribers) {
                EmptySubscription.error(iae, s);
            }
            return false;
        }
        return true;
    }
}