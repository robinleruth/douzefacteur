package io.robin.douzefacteur.domain.admin;

import org.apache.pekko.actor.AbstractLoggingActor;
import org.apache.pekko.actor.Props;

import java.io.Serializable;

public class AdminActor extends AbstractLoggingActor {

    public static Props props() {
        return Props.create(AdminActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Event.AdminMessageSent.class, this::onAdminMessageSent)
                .build();
    }

    private void onAdminMessageSent(Event.AdminMessageSent o) {
        log().info("AdminActorSingleton received event {}", o);
        sender().tell(new Event.AdminMessageAcked(o.msg()), self());
    }


    public interface Event {
        record AdminMessageSent(String msg) implements Serializable {}
        record AdminMessageAcked(String msg) implements Serializable {}
    }
}
