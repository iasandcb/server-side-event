package com.ktb.spring;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

// Optional: Use @CrossOrigin if not using global CORS config
@CrossOrigin(origins = "http://localhost:3000") // Allow requests from Next.js dev server
@RestController
@RequestMapping("/api")
public class SseController {

    private final WebClient webClient;
    // Use a Sink to push events from the FastAPI stream to the SseEmitter stream
    // Sinks.Many is suitable for broadcasting or multicasting
    private final Many<ServerSentEvent<String>> sink = Sinks.many().multicast().onBackpressureBuffer();

    public SseController() {
        // Configure WebClient to connect to the FastAPI service
        this.webClient = WebClient.builder().baseUrl("http://localhost:8000").build(); // FastAPI URL
        // Immediately start consuming from FastAPI and push to the sink
    }

    private void startConsumingFromFastAPI() {
        System.out.println("Spring Boot: Connecting to FastAPI SSE stream...");
        webClient.get()
                 .uri("/stream") // FastAPI SSE endpoint
                 .retrieve()
                 // Consume the response body as a stream of ServerSentEvent
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {}) // 이 줄 수정
                .doOnNext(event -> {
                     // Log received event from FastAPI
                     System.out.println("Spring Boot received from FastAPI: " + event.data());
                     // Push the received event data into our sink
                     // We wrap the data in a new ServerSentEvent for the Next.js client
                     sink.tryEmitNext(ServerSentEvent.<String>builder().data((String) event.data()).build());
                 })
                 .doOnError(error -> {
                     System.err.println("Spring Boot error consuming FastAPI stream: " + error);
                     // Optionally emit an error event to the sink
                     // sink.tryEmitError(error); // This would terminate the sink
                     // Or just log and let individual SseEmitters handle their errors
                 })
                 .doOnComplete(() -> {
                     System.out.println("Spring Boot finished consuming FastAPI stream.");
                     // Optionally complete the sink if the FastAPI stream finishing
                     // means all clients should also finish.
                     // sink.tryEmitComplete();
                 })
                 .subscribe(); // Subscribe to start the flux
    }

    @GetMapping(value = "/process", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> processAi() {
        System.out.println("Spring Boot: Client connected to /api/process. Starting SSE stream...");

        startConsumingFromFastAPI();

        // Return the flux from the sink.
        // When a client connects, they subscribe to this flux,
        // receiving events that are pushed into the sink from the FastAPI stream.
        return sink.asFlux()
                   .doOnCancel(() -> System.out.println("Spring Boot: Client disconnected from /api/process."))
                   .doOnError(error -> System.err.println("Spring Boot: Error on /api/process stream: " + error));
    }
}