package io.robin.douzefacteur;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.robin.douzefacteur.domain.TestComputeActor;
import io.robin.douzefacteur.domain.admin.AdminActor;
import io.robin.douzefacteur.domain.admin.AdminSingletonAwareActor;
import io.robin.douzefacteur.domain.admin.ClusterListenerActor;
import io.robin.douzefacteur.domain.compute.ComputeActor;
import org.apache.pekko.Done;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.CoordinatedShutdown;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.cluster.sharding.ClusterSharding;
import org.apache.pekko.cluster.sharding.ClusterShardingSettings;
import org.apache.pekko.cluster.singleton.ClusterSingletonManager;
import org.apache.pekko.cluster.singleton.ClusterSingletonManagerSettings;
import org.apache.pekko.cluster.singleton.ClusterSingletonProxy;
import org.apache.pekko.cluster.singleton.ClusterSingletonProxySettings;
import org.apache.pekko.management.javadsl.PekkoManagement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Component
public class ActorSystem implements ApplicationListener<ApplicationReadyEvent> {
    private final int actorSystemPort;
    private final List<String> springProfiles;
    private org.apache.pekko.actor.ActorSystem actorSystem;
    private ActorRef computeShardingRegion;

    public ActorSystem(@Value("${act.system.port}") int actorSystemPort, @Value("${spring.profiles.active}") List<String> springProfiles) {
        this.actorSystemPort = actorSystemPort;
        this.springProfiles = springProfiles;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        actorSystem = org.apache.pekko.actor.ActorSystem.create("cluster", setupClusterNodeConfig(actorSystemPort));
        PekkoManagement pekkoManagement = PekkoManagement.get(actorSystem);
        pekkoManagement.start();

        actorSystem.actorOf(ClusterListenerActor.props(), "clusterListener");
        /*
         * Set up Sharding for each of the roles
         * Node without roles will only have read-only proxy ShardRegion
         */

        // ==== ADMIN ====
        // Admin Singleton
        Props adminSingletonProps = ClusterSingletonManager.props(
                AdminActor.props(),
                PoisonPill.getInstance(),
                ClusterSingletonManagerSettings.create(actorSystem)
        );
        actorSystem.actorOf(adminSingletonProps, "adminSingletonManager"); // unused actor ref but normal
        Props adminSingletonProxyProps = ClusterSingletonProxy.props("/user/adminSingletonManager", ClusterSingletonProxySettings.create(actorSystem));
        ActorRef adminSingletonProxy = actorSystem.actorOf(adminSingletonProxyProps, "adminSingletonProxy");
        actorSystem.actorOf(AdminSingletonAwareActor.props(adminSingletonProxy), "adminSingletonAwareActor");


        // ==== COMPUTE ====
        var computeClusterShardingSettings = ClusterShardingSettings.create(actorSystem).withRole("compute");
        computeShardingRegion = ClusterSharding.get(actorSystem).start(
                "compute",
                ComputeActor.props(),
                computeClusterShardingSettings,
                ComputeActor.Message.messageExtractor()
        );
        actorSystem.actorOf(TestComputeActor.props(computeShardingRegion), "testComputeActor");

        addCoordinatedShutdownTask(CoordinatedShutdown.PhaseClusterShutdown());

        actorSystem.log().info("Pekko node {}", actorSystem.provider().getDefaultAddress());
    }

    private void addCoordinatedShutdownTask(String s) {
        CoordinatedShutdown.get(actorSystem).addTask(
                s,
                s,
                () -> {
                    actorSystem.log().warning("Coordinated shutdown phase {}", s);
                    return CompletableFuture.completedFuture(Done.getInstance());
                }
        );

    }

    private Config setupClusterNodeConfig(int actorSystemPort) {
        return ConfigFactory.parseString(
                        String.format("pekko.remote.netty.tcp.port=%s%n", actorSystemPort) +
                                String.format("pekko.remote.artery.canonical.port=%s%n", actorSystemPort) +
                                String.format("pekko.cluster.roles=[%s]%n", springProfiles.stream().map(profile -> "\"" + profile + "\"").collect(Collectors.joining(",")))
                )
                .withFallback(ConfigFactory.load());
    }

    public void tell(Object commandOrEvent) {
        actorSystem.eventStream().publish(commandOrEvent);
    }
}
