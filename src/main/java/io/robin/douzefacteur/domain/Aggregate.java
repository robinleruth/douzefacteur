package io.robin.douzefacteur.domain;

import java.io.Serializable;
import java.util.Collection;

public interface Aggregate extends Serializable {
    Collection<Event> checkCommand(Command command);

    void applyEvent(Event event);

    interface Command {}

    interface Event {}
}
