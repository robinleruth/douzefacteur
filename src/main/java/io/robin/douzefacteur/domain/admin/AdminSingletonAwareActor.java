package io.robin.douzefacteur.domain.admin;

import org.apache.pekko.actor.AbstractLoggingActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;

public class AdminSingletonAwareActor extends AbstractLoggingActor {
    private final ActorRef adminSingletonProxy;

    public AdminSingletonAwareActor(ActorRef adminSingletonProxy) {
        this.adminSingletonProxy = adminSingletonProxy;
    }

    public static Props props(ActorRef adminSingletonProxy) {
        return Props.create(AdminSingletonAwareActor.class, () -> new AdminSingletonAwareActor(adminSingletonProxy));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AdminActor.Event.AdminMessageSent.class, this::forwardToAdminActor)
                .match(AdminActor.Event.AdminMessageAcked.class, this::logAcknowledgement)
                .build();
    }

    private void logAcknowledgement(AdminActor.Event.AdminMessageAcked adminMessageAcked) {
        log().info("Acknowledgement received from Admin Singleton Actor");
    }

    @Override
    public void preStart() throws Exception {
        context().system().eventStream().subscribe(self(), AdminActor.Event.AdminMessageSent.class);
    }

    private void forwardToAdminActor(AdminActor.Event.AdminMessageSent o) {
        adminSingletonProxy.tell(o, self());
    }
}
