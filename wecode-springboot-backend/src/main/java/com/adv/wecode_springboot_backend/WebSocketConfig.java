package com.adv.wecode_springboot_backend;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // This is the endpoint clients will connect to for the WebSocket handshake.
        // It's crucial to allow origins from your React development server.
        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:5173") // Your React app's origin
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Defines prefixes for topics the server will broadcast to.
        // Your React client subscribes to destinations like "/topic/room/sync/{roomId}".
        config.enableSimpleBroker("/topic");

        // Defines the prefix for destinations that clients send messages to.
        // Your React client sends messages to destinations like "/app/room/sync/{roomId}".
        config.setApplicationDestinationPrefixes("/app");
    }
}
