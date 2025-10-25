package io.robin.douzefacteur.domain;

import io.robin.douzefacteur.domain.compute.ComputeActor;
import org.apache.pekko.actor.AbstractLoggingActor;
import org.apache.pekko.actor.ActorRef;
import org.apache.pekko.actor.Props;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;

public class TestComputeActor extends AbstractLoggingActor {
    private final ActorRef computeActor;
    private int batch = 0;
    private int partition = 0;
    private static final List<Integer> integersToSum = IntStream.range(0, 1000).boxed().toList();
    private final Map<Integer, Map<Integer, Optional<Integer>>> resultOptByPartitionIdByBatchId = new HashMap<>();

    public TestComputeActor(ActorRef computeActor) {
        this.computeActor = computeActor;
    }

    public static Props props(ActorRef computeActor) {
        return Props.create(TestComputeActor.class, () -> new TestComputeActor(computeActor));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Command.class, this::doCommand)
                .match(ComputeActor.Event.class, this::handleComputeEvent)
                .build();
    }

    @Override
    public void preStart() throws Exception {
        context().system().eventStream().subscribe(self(), Command.class);
    }

    private void handleComputeEvent(ComputeActor.Event event) {
        switch (event) {
            case ComputeActor.Event.SumDone sumDone -> {
                var resultOptByPartitionId = resultOptByPartitionIdByBatchId.get(sumDone.batchId());
                resultOptByPartitionId.put(sumDone.id(), Optional.of(sumDone.result()));
                log().info("Received result for batch Id {} partition id {}", sumDone.batchId(), sumDone.id());
                if (resultOptByPartitionId.values().stream().allMatch(Optional::isPresent)) {
                    log().info("Received all partitions of batch Id {} : {}", sumDone.batchId(), resultOptByPartitionId.values().stream().mapToInt(Optional::get).sum());
                }
            }
        }
    }

    private void doCommand(Command command) {
        switch (command) {
            case Command.TriggerCompute triggerCompute -> {
                int batchId = ++batch;
                var resultOptByPartitionId = resultOptByPartitionIdByBatchId.computeIfAbsent(batchId, k -> new HashMap<>());
                for (int i = 0; i < integersToSum.size(); i += 100) {
                    int[] integers = integersToSum.subList(i, i + 100).stream().mapToInt(j -> j).toArray();
                    int id = ++partition;
                    resultOptByPartitionId.put(id, Optional.empty());
                    log().info("Send compute request to ComputeActor for batch id {} partition id {}", batchId, id);
                    computeActor.tell(new ComputeActor.Command.DoSum(batchId, id, integers), self());
                }
            }
        }
    }

    public sealed interface Command permits Command.TriggerCompute {
        record TriggerCompute() implements Command, Serializable { }
    }
}
