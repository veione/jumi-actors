// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.actors;

import javax.annotation.concurrent.*;
import java.util.concurrent.Executor;

@ThreadSafe
public abstract class Actors implements LongLivedActors, OnDemandActors {

    private final Eventizer<?>[] factories;
    private final ThreadLocal<ActorThreadImpl> currentActorThread = new ThreadLocal<ActorThreadImpl>();

    public Actors(Eventizer<?>... factories) {
        this.factories = factories;
    }

    @Override
    public <T> ActorRef<T> createPrimaryActor(Class<T> type, T target, String name) {
        ActorThread actorThread = startActorThread(name);
        return actorThread.createActor(type, target);
    }

    public ActorThread startActorThread(String name) {
        checkNotInsideAnActor();
        ActorThreadImpl actorThread = new ActorThreadImpl();
        startActorThread(name, actorThread);
        return actorThread;
    }

    private void checkNotInsideAnActor() {
        if (currentActorThread.get() != null) {
            throw new IllegalStateException("already inside an actor");
        }
    }

    protected abstract void startActorThread(String name, ActorThreadImpl actorThread);

    @Override
    public void startUnattendedWorker(Runnable worker, Runnable onFinished) {
        ActorRef<Runnable> onFinishedHandle = createSecondaryActor(Runnable.class, onFinished);
        doStartUnattendedWorker(new UnattendedWorker(worker, onFinishedHandle));
    }

    protected abstract void doStartUnattendedWorker(Runnable worker);

    @Override
    public <T> ActorRef<T> createSecondaryActor(Class<T> type, T target) {
        ActorThread actorThread = getCurrentActorThread();
        return actorThread.createActor(type, target);
    }

    private ActorThreadImpl getCurrentActorThread() {
        ActorThreadImpl actorThread = currentActorThread.get();
        if (actorThread == null) {
            throw new IllegalStateException("We are not inside an actor");
        }
        return actorThread;
    }

    @SuppressWarnings({"unchecked"})
    private <T> Eventizer<T> getFactoryForType(Class<T> type) {
        for (Eventizer<?> factory : factories) {
            if (factory.getType().equals(type)) {
                return (Eventizer<T>) factory;
            }
        }
        throw new IllegalArgumentException("unsupported listener type: " + type);
    }


    @ThreadSafe
    protected class ActorThreadImpl implements Executor, ActorThread {
        private final MessageQueue<Runnable> taskQueue = new MessageQueue<Runnable>();

        @Override
        public <T> ActorRef<T> createActor(Class<T> type, T target) {
            Eventizer<T> factory = getFactoryForType(type);
            T proxy = factory.newFrontend(new MessageToActorSender<T>(this, target));
            return ActorRef.wrap(type.cast(proxy));
        }

        @Override
        public void execute(Runnable task) {
            taskQueue.send(task);
        }

        public void processNextMessage() throws InterruptedException {
            Runnable task = taskQueue.take();
            process(task);
        }

        public boolean processNextMessageIfAny() {
            Runnable task = taskQueue.poll();
            if (task == null) {
                return false;
            }
            process(task);
            return true;
        }

        private void process(Runnable task) {
            currentActorThread.set(this);
            try {
                task.run();
            } finally {
                currentActorThread.remove();
            }
        }
    }

    @NotThreadSafe
    private static class UnattendedWorker implements Runnable { // TODO: decouple workers from actors
        private final Runnable worker;
        private final ActorRef<Runnable> onFinished;

        public UnattendedWorker(Runnable worker, ActorRef<Runnable> onFinished) {
            this.worker = worker;
            this.onFinished = onFinished;
        }

        @Override
        public void run() {
            try {
                worker.run();
            } finally {
                onFinished.tell().run();
            }
        }
    }

    @ThreadSafe
    private static class MessageToActorSender<T> implements MessageSender<Event<T>> {
        private final Executor actorThread;
        private final T rawActor;

        public MessageToActorSender(Executor actorThread, T rawActor) {
            this.actorThread = actorThread;
            this.rawActor = rawActor;
        }

        @Override
        public void send(final Event<T> message) {
            actorThread.execute(new MessageToActor<T>(rawActor, message));
        }
    }

    @NotThreadSafe
    private static class MessageToActor<T> implements Runnable {
        private T rawActor;
        private final Event<T> message;

        public MessageToActor(T rawActor, Event<T> message) {
            this.rawActor = rawActor;
            this.message = message;
        }

        @Override
        public void run() {
            message.fireOn(rawActor);
        }
    }
}
