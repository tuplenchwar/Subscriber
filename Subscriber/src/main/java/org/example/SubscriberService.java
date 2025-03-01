package org.example;

import dto.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class SubscriberService {

    private final RestTemplate restTemplate;

    @Value("${coordinator.url:http://localhost:8080}")
    private String coordinatorUrl;

    private String leaderBrokerUrl;
    private final Integer subscriberId;
    private final String subscriberUrl; // URL where this subscriber receives messages
    private final CopyOnWriteArrayList<String> topics = new CopyOnWriteArrayList<>();

    @Autowired
    public SubscriberService(RestTemplate restTemplate, @Value("${server.port}") String serverPort) {
        this.restTemplate = restTemplate;
        UUID uuid = UUID.randomUUID();
        this.subscriberId = uuid.hashCode() & 0x7fffffff;
        this.subscriberUrl = "http://localhost:" + serverPort;
        System.out.println("Subscriber started with ID: " + this.subscriberId);
    }

    public void initialize() {
        fetchLeaderBroker();
        registerSubscriberOnce();
        startSubscriberConsole();
    }

    private void fetchLeaderBroker() {
        String leaderUrl = coordinatorUrl + "/coordinator/leader";
        ResponseEntity<Broker> response = restTemplate.getForEntity(leaderUrl, Broker.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            leaderBrokerUrl = response.getBody().getConnectionUrl();
            System.out.println("Leader Broker URL: " + leaderBrokerUrl);
        } else {
            throw new RuntimeException("Failed to fetch leader broker from coordinator.");
        }
    }

    private void registerSubscriberOnce() {
        // Check if already registered
            Subscriber subscriber = new Subscriber(subscriberId, null, null, subscriberUrl);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Subscriber> request = new HttpEntity<>(subscriber, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    leaderBrokerUrl + "/broker/register-subscriber", request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("Subscriber successfully registered with broker.");
            } else {
                throw new RuntimeException("Failed to register subscriber.");
            }
    }


    public CopyOnWriteArrayList<String> getAvailableTopics(Integer subscriberId) {
        int retryCount = 0;
        int maxRetries = 3; // Prevent infinite loop

        while (retryCount < maxRetries) {
            if (leaderBrokerUrl == null) {
                fetchLeaderBroker();
            }

            if (leaderBrokerUrl == null) { // If leader fetch fails
                System.out.println("Failed to determine leader broker. Attempt: " + (retryCount + 1));
                retryCount++;
                continue; // Try fetching the leader again
            }

            try {
                String url = leaderBrokerUrl + "/broker/gettopics?subscriberId=" + subscriberId;
                ResponseEntity<CopyOnWriteArrayList> response = restTemplate.getForEntity(url, CopyOnWriteArrayList.class);

                if (response.getStatusCode() == HttpStatus.FORBIDDEN) {
                    System.out.println("Subscriber ID is not registered.");
                    return new CopyOnWriteArrayList<>();
                }

                CopyOnWriteArrayList<String> topics = response.getBody();
                if (topics == null) {
                    System.out.println("Received null topic list from broker.");
                    return new CopyOnWriteArrayList<>();
                }

                System.out.println("Available Topics: " + topics);
                return topics;
            } catch (Exception e) {
                System.out.println("Leader broker unavailable. Attempt: " + (retryCount + 1));
                fetchLeaderBroker(); // Fetch new leader
                retryCount++;
            }
        }

        System.out.println("No leader broker found after " + maxRetries + " retries. Returning empty topic list.");
        return new CopyOnWriteArrayList<>();
    }



    public void subscribeToTopic(String topic) {
        if (leaderBrokerUrl == null) {
            fetchLeaderBroker();
        }

        if (leaderBrokerUrl == null) { // If leader fetch fails
            System.out.println("Failed to determine leader broker. Subscription aborted.");
            return;
        }

        try {
            if (!topics.contains(topic)) {
                Subscriber subscriber = new Subscriber(subscriberId, topic, null, subscriberUrl);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Subscriber> request = new HttpEntity<>(subscriber, headers);

                System.out.println("Sending subscribe request to broker: " + leaderBrokerUrl);
                ResponseEntity<String> response = restTemplate.exchange(
                        leaderBrokerUrl + "/broker/subscribe", HttpMethod.PUT, request, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    topics.add(topic);
                    System.out.println("Successfully subscribed to topic: " + topic);
                } else {
                    System.out.println("Subscription failed for topic: " + topic + ". Response: " + response.getStatusCode());
                }
            } else {
                System.out.println("Already subscribed to topic: " + topic);
            }
        } catch (Exception e) {
            System.out.println("Subscription request failed: " + e.getMessage());
            System.out.println("Leader broker unavailable. Fetching new leader...");
            fetchLeaderBroker();

            // Prevent infinite recursion if leader fetching fails repeatedly
            if (leaderBrokerUrl == null) {
                System.out.println("No leader broker found after retry. Subscription aborted.");
                return;
            }
            subscribeToTopic(topic); // Retry with new leader
        }
    }


    public void unsubscribeFromTopic(String topic) {
        if (leaderBrokerUrl == null) {
            fetchLeaderBroker();
        }

        if (leaderBrokerUrl == null) { // If leader fetch fails
            System.out.println("Failed to determine leader broker. Unsubscription aborted.");
            return;
        }

        try {
            if (topics.contains(topic)) {
                Subscriber subscriber = new Subscriber(subscriberId, topic, null, subscriberUrl);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Subscriber> request = new HttpEntity<>(subscriber, headers);

                System.out.println("Sending unsubscribe request to broker: " + leaderBrokerUrl);
                ResponseEntity<String> response = restTemplate.exchange(
                        leaderBrokerUrl + "/broker/unsubscribe", HttpMethod.PUT, request, String.class);

                if (response.getStatusCode() == HttpStatus.OK) {
                    topics.remove(topic);
                    System.out.println("Successfully unsubscribed from topic: " + topic);
                } else {
                    System.out.println("Unsubscription failed for topic: " + topic + ". Response: " + response.getStatusCode());
                }
            } else {
                System.out.println("Not subscribed to topic: " + topic);
            }
        } catch (Exception e) {
            System.out.println("Unsubscription request failed: " + e.getMessage());
            System.out.println("Leader broker unavailable. Fetching new leader...");
            fetchLeaderBroker();

            // Prevent infinite recursion if leader fetching fails repeatedly
            if (leaderBrokerUrl == null) {
                System.out.println("No leader broker found after retry. Unsubscription aborted.");
                return;
            }
            unsubscribeFromTopic(topic); // Retry with new leader
        }
    }


    public void receiveMessage(Packet message) {
        System.out.println("Received Message:");
        System.out.println("Topic: " + message.getTopic());
        System.out.println("Message: " + message.getMessage());
        System.out.println("Publisher ID: " + message.getPid());
        System.out.println("Timestamp: " + message.getTimestamp());
    }

    private void startSubscriberConsole() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("\nSubscriber Menu:");
            System.out.println("1] Get available topics");
            System.out.println("2] Subscribe to a topic");
            System.out.println("3] Unsubscribe from a topic");
            System.out.println("4] Exit");
            System.out.print("Enter your choice: ");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    CopyOnWriteArrayList<String> topics = getAvailableTopics(subscriberId);
                    break;
                case 2:
                    System.out.print("Enter topic to subscribe: ");
                    String topic = scanner.nextLine();
                    subscribeToTopic(topic);
                    break;
                case 3:
                    System.out.print("Enter topic to unsubscribe: ");
                    String unsubscribeTopic = scanner.nextLine();
                    unsubscribeFromTopic(unsubscribeTopic);
                    break;
                case 4:
                    System.out.println("Exiting subscriber...");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }
}
