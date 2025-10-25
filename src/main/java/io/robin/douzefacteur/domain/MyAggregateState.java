package io.robin.douzefacteur.domain;


import java.util.Collection;
import java.util.List;

public class MyAggregateState implements Aggregate {
    private final Integer aggregateId;
    private Integer mySum;
    private boolean isInit = false;

    public MyAggregateState(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    @Override
    public Collection<Event> checkCommand(Command command) {
        return switch (command) {
            case InitializeMyAggregate initializeMyAggregate -> List.of(new MyAggregateInitialized());
            case DoSum doSum -> List.of(new Incremented(doSum.amount()));
            default -> throw new IllegalStateException("Unexpected value: " + command);
        };
    }

    @Override
    public void applyEvent(Event event) {
        switch (event) {
            case MyAggregateInitialized myAggregateInitialized -> {
                if (isInit) {
                    return;
                }
                mySum = 0;
                isInit = true;
                // log.info("MyAggregateState initialized");
            }
            case Incremented incremented -> {
                mySum += incremented.amount();
                // log.info("Incremented by {}", incremented.amount());
            }
            default -> throw new IllegalStateException("Unexpected value: " + event);
        }
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public record InitializeMyAggregate() implements Command { }
    public record DoSum(Integer aggregateId, Integer amount) implements Command { }
    public record MyAggregateInitialized() implements Event { }
    public record Incremented(Integer amount) implements Event { }
}
