package io.robin.douzefacteur.domain.admin;

import org.apache.pekko.actor.AbstractLoggingActor;
import org.apache.pekko.actor.Cancellable;
import org.apache.pekko.actor.Props;
import org.apache.pekko.cluster.Cluster;
import org.apache.pekko.cluster.ClusterEvent;
import org.apache.pekko.cluster.Member;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

public class ClusterListenerActor extends AbstractLoggingActor {
    private final Cluster cluster = Cluster.get(context().system());
    private Cancellable showClusterStateCancelable;

    public static Props props() {
        return Props.create(ClusterListenerActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ShowClusterState.class, this::showClusterState)
                .matchAny(this::logClusterEvent)
                .build();
    }

    @Override
    public void preStart() throws Exception {
        log().info("Start");
        cluster.subscribe(self(), ClusterEvent.initialStateAsEvents(),
                ClusterEvent.ClusterDomainEvent.class);
    }

    @Override
    public void postStop() throws Exception {
        log().info("Stop");
        cluster.unsubscribe(self());
    }

    private void logClusterEvent(Object o) {
        log().info("{} sent to {}", o, cluster.selfMember());
        logClusterMembers();
    }

    private void showClusterState(ShowClusterState showClusterState) {
        log().info("{} sent to {}", showClusterState, cluster.selfMember());
        logClusterMembers(cluster.state());
        showClusterStateCancelable = null;
    }

    private void logClusterMembers() {
        ClusterEvent.CurrentClusterState state = cluster.state();
        logClusterMembers(state);

        if (showClusterStateCancelable == null) {
            showClusterStateCancelable = context().system().scheduler().scheduleOnce(
                    Duration.ofSeconds(15),
                    self(),
                    new ShowClusterState(),
                    context().system().dispatcher(),
                    null);
        }
    }

    private void logClusterMembers(ClusterEvent.CurrentClusterState currentClusterState) {
        Optional<Member> old = StreamSupport.stream(currentClusterState.getMembers().spliterator(), false)
                .reduce((older, member) -> older.isOlderThan(member) ? older : member);

        Member oldest = old.orElse(cluster.selfMember());

        StreamSupport.stream(currentClusterState.getMembers().spliterator(), false)
                .forEach(new Consumer<Member>() {
                    int m = 0;

                    @Override
                    public void accept(Member member) {
                        log().info("{} {}{}{}", ++m, leader(member), oldest(member), member.toString() + " with roles " + member.getRoles());
                    }

                    private String leader(Member member) {
                        return member.address().equals(currentClusterState.getLeader()) ? "(LEADER) " : "";
                    }

                    private String oldest(Member member) {
                        return oldest.equals(member) ? "(OLDEST) " : "";
                    }
                });
    }


    private static class ShowClusterState {
        @Override
        public String toString() {
            return ShowClusterState.class.getSimpleName();
        }
    }
}
