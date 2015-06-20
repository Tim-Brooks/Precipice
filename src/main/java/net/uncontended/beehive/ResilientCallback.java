package net.uncontended.beehive;

import net.uncontended.beehive.concurrent.ResilientPromise;

/**
 * Created by timbrooks on 1/17/15.
 */
public interface ResilientCallback<T> {

    void run(ResilientPromise<T> resultPromise);
}
