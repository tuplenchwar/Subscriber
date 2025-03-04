/*package org.example;

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
        // TODO: add message to queue
    }

    @GetMapping("/receiveMessages")
    public void receiveMessages() {
        // TODO: return all the messages in the queue (dequeue them)
    }



}


package org.example;

import dto.Packet;
import org.springframework.web.bind.annotation.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping("/subscriber")
public class SubscriberController {

    private final SubscriberService subscriberService;
    private final Queue<Packet> messageQueue = new ConcurrentLinkedQueue<>(); // Queue for fresh messages

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping("/receive")
    public void receiveMessage(@RequestBody Packet message) {
        subscriberService.receiveMessage(message);
        messageQueue.add(message); // Add new message to queue
    }

    @GetMapping("/receiveMessages")
    public Queue<Packet> receiveMessages() {
        Queue<Packet> freshMessages = new ConcurrentLinkedQueue<>(messageQueue);
        messageQueue.clear(); // Clear queue after fetching
        return freshMessages; // Return only new messages
    }


}*/
/*package org.example;

import dto.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@RestController
@RequestMapping("/subscriber")
public class SubscriberController {

    private final SubscriberService subscriberService;
    private final Queue<Packet> messageQueue = new ConcurrentLinkedQueue<>(); // Queue for fresh messages
    private final RestTemplate restTemplate = new RestTemplate();

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping("/receive")
    public void receiveMessage(@RequestBody Packet message) {
        subscriberService.receiveMessage(message);
        messageQueue.add(message); // Add new message to queue
    }

    @GetMapping("/receiveMessages")
    public Queue<Packet> receiveMessages() {
        Queue<Packet> freshMessages = new ConcurrentLinkedQueue<>(messageQueue);
        messageQueue.clear(); // Clear queue after fetching
        return freshMessages; // Return only new messages
    }

    @GetMapping("/getTopics")
    public ResponseEntity<List<String>> getAvailableTopics(@RequestParam("subscriberUrl") String subscriberUrl) {
        List<String> topics = subscriberService.getAvailableTopics(subscriberUrl);
        return ResponseEntity.ok(topics);
    }

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribeToTopic(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        if (topic == null) {
            return ResponseEntity.badRequest().body("Invalid request: topic is required.");
        }

        subscriberService.subscribeToTopic(topic);
        return ResponseEntity.ok("Subscribed successfully to: " + topic);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribeFromTopic(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        if (topic == null) {
            return ResponseEntity.badRequest().body("Invalid request: topic is required.");
        }

        subscriberService.unsubscribeFromTopic(topic);
        return ResponseEntity.ok("Unsubscribed successfully from: " + topic);
    }
}*/

package org.example;

import dto.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@CrossOrigin(origins = "http://localhost:3000") // Allow React frontend access
@RestController
@RequestMapping("/subscriber")
public class SubscriberController {

    private final SubscriberService subscriberService;
    private final Queue<Packet> messageQueue = new ConcurrentLinkedQueue<>();

    public SubscriberController(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @PostMapping("/receive")
    public void receiveMessage(@RequestBody Packet message) {
        subscriberService.receiveMessage(message);
        messageQueue.add(message);
    }

    @GetMapping("/receiveMessages")
    public ResponseEntity<Queue<Packet>> receiveMessages() {
        Queue<Packet> freshMessages = new ConcurrentLinkedQueue<>(messageQueue);
        messageQueue.clear();
        return ResponseEntity.ok(freshMessages);
    }

    @GetMapping("/getTopics")
    public ResponseEntity<List<String>> getAvailableTopics(@RequestParam("subscriberUrl") String subscriberUrl) {
        List<String> topics = subscriberService.getAvailableTopics(subscriberUrl);
        return ResponseEntity.ok(topics);
    }

    @GetMapping("/getSubscriberId")
    public Integer getSubscriberId() {
        return subscriberService.getSubscriberId();
    }


    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribeToTopic(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        if (topic == null) {
            return ResponseEntity.badRequest().body("Invalid request: topic is required.");
        }

        subscriberService.subscribeToTopic(topic);
        return ResponseEntity.ok("Subscribed successfully to: " + topic);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribeFromTopic(@RequestBody Map<String, String> request) {
        String topic = request.get("topic");
        if (topic == null) {
            return ResponseEntity.badRequest().body("Invalid request: topic is required.");
        }

        subscriberService.unsubscribeFromTopic(topic);
        return ResponseEntity.ok("Unsubscribed successfully from: " + topic);
    }
}











