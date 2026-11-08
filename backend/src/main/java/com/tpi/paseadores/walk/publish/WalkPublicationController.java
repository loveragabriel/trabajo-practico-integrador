package com.tpi.paseadores.walk.publish;

import com.tpi.paseadores.walk.publish.dto.WalkPublishRequest;
import com.tpi.paseadores.walk.publish.dto.WalkPublishResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/walks")
public class WalkPublicationController {
    private final WalkPublicationService service;

    public WalkPublicationController(WalkPublicationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<WalkPublishResponse> publish(@AuthenticationPrincipal String userId,
                                                       @Valid @RequestBody WalkPublishRequest request) {
        var created = service.publish(UUID.fromString(userId), request);
        return ResponseEntity.created(URI.create("/api/walks/" + created.id())).body(created);
    }
}
