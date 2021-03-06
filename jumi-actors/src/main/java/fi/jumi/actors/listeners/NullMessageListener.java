// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.actors.listeners;

import javax.annotation.concurrent.Immutable;
import java.util.concurrent.Executor;

/**
 * Does nothing. Meant for production use.
 */
@Immutable
public class NullMessageListener implements MessageListener {

    @Override
    public void onMessageSent(Object message) {
    }

    @Override
    public void onProcessingStarted(Object actor, Object message) {
    }

    @Override
    public void onProcessingFinished() {
    }

    @Override
    public Executor getListenedExecutor(Executor realExecutor) {
        return realExecutor;
    }
}
