package io.robin.douzefacteur.domain;

import org.apache.pekko.actor.Props;

public class MyAggregateActor extends AggregateActor<MyAggregateState> {
    public static Props props(Integer id) {
        return Props.create(MyAggregateActor.class, () -> new MyAggregateActor(id));
    }

    public MyAggregateActor(Integer id) {
        aggregate = new MyAggregateState(id);
    }

    @Override
    public String persistenceId() {
        return "MyAggregate - " + aggregate.getAggregateId().toString();
    }
}
