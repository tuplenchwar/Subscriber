package org.example;

import dto.Packet;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscriber")
public class SubscriberController {

    private final SubscriberService subscriberService;

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping("/receive")
    public void receiveMessage(@RequestBody Packet message) {
        subscriberService.receiveMessage(message);
    }
}








