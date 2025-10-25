package io.robin.douzefacteur.domain;

import org.apache.pekko.persistence.AbstractPersistentActor;
import org.apache.pekko.persistence.SnapshotOffer;

import java.util.Collection;

public abstract class AggregateActor<T extends Aggregate> extends AbstractPersistentActor {
    protected T aggregate;
    private int count = 0;

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(Aggregate.Event.class, aggregate::applyEvent)
                .match(SnapshotOffer.class, ss -> aggregate = (T) ss.snapshot())
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Aggregate.Command.class, command -> {
                    Collection<Aggregate.Event> events = aggregate.checkCommand(command);
                    for (Aggregate.Event event : events) {
                        persistAsync(event, aggregate::applyEvent);
                        // getContext().getSystem().getEventStream().publish(event);

                        if (count++ % 5 == 0) {
                            saveSnapshot(aggregate);
                        }
                    }
                })
                .build();
    }
}
