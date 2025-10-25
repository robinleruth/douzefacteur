package io.robin.douzefacteur.interfaces.api.rest;

import io.robin.douzefacteur.ActorSystem;
import io.robin.douzefacteur.domain.TestComputeActor;
import io.robin.douzefacteur.domain.admin.AdminActor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {
    private final ActorSystem actorSystem;

    public AdminController(ActorSystem actorSystem) {
        this.actorSystem = actorSystem;
    }

    @GetMapping(value = "/v1/health")
    public ResponseEntity<String> getHealth() {
        return ResponseEntity.ok("Up");
    }

    @GetMapping(value = "/v1/adminCommand")
    public ResponseEntity<String> adminCommand() {
        actorSystem.tell(new AdminActor.Event.AdminMessageSent("Message from Controller !"));
        return ResponseEntity.ok("Sent");
    }

    @GetMapping(value = "/v1/compute")
    public ResponseEntity<String> computeRequest() {
        actorSystem.tell(new TestComputeActor.Command.TriggerCompute());
        return ResponseEntity.ok("Sent");
    }
}
