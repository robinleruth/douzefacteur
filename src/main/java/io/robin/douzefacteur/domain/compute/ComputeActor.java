package io.robin.douzefacteur.domain.compute;

import org.apache.pekko.actor.AbstractLoggingActor;
import org.apache.pekko.actor.PoisonPill;
import org.apache.pekko.actor.Props;
import org.apache.pekko.actor.ReceiveTimeout;
import org.apache.pekko.cluster.sharding.ShardRegion;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class ComputeActor extends AbstractLoggingActor {
    private final FiniteDuration receiveTimeout = Duration.create(60, TimeUnit.SECONDS);

    public static Props props() {
        return Props.create(ComputeActor.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.class, this::handleCommand)
                .matchEquals(ReceiveTimeout.getInstance(), t -> passivate())
                .build();
    }

    private void handleCommand(Command command) {
        switch (command) {
            case Command.DoSum doSum -> {
                int sum = Arrays.stream(doSum.integers).sum();
                log().info("Received compute request for batch id {}, interim result : {}", doSum.batchId, sum);
                sender().tell(new Event.SumDone(doSum.batchId, doSum.id, sum), self());
            }
        }
    }

    @Override
    public void preStart() throws Exception {
        log().info("Compute actor started");
        context().setReceiveTimeout(receiveTimeout);
    }

    @Override
    public void postStop() throws Exception {
        log().info("Stop ComputeActor");
    }

    private void passivate() {
        context().parent().tell(new ShardRegion.Passivate(PoisonPill.getInstance()), self());
    }

    public sealed interface Command permits Command.DoSum {
        record DoSum(Integer batchId, Integer id, int[] integers) implements Command, Serializable { }
    }

    public sealed interface Event permits Event.SumDone {
        record SumDone(Integer batchId, Integer id, int result) implements Event, Serializable { }
    }

    public interface Message {
        static ShardRegion.MessageExtractor messageExtractor() {
            return new ShardRegion.MessageExtractor() {
                final int numberOfShards = 1024;
                final int numberOfActorPerShards = 8;
                final Random random = new Random();

                @Override
                public String entityId(Object message) {
                    return String.valueOf(random.nextInt() ^ (numberOfActorPerShards - 1));
                }

                @Override
                public Object entityMessage(Object message) {
                    return message;
                }

                @Override
                public String shardId(Object message) {
                    return String.valueOf(random.nextInt() ^ (numberOfShards - 1));
                }
            };
        }
    }
}
