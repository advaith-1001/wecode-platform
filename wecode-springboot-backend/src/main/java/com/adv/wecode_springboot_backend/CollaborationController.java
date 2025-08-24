//package com.adv.wecode_springboot_backend;
//
//import org.springframework.messaging.handler.annotation.DestinationVariable;
//import org.springframework.messaging.handler.annotation.MessageMapping;
//import org.springframework.messaging.handler.annotation.Payload;
//import org.springframework.messaging.handler.annotation.SendTo;
//import org.springframework.stereotype.Controller;
//import org.springframework.web.bind.annotation.CrossOrigin;
//
//@Controller
//public class CollaborationController {
//
//    /**
//     * Handles the ongoing, real-time synchronization of small changes (deltas).
//     * This is the endpoint for continuous collaboration after initialization.
//     */
//    @MessageMapping("/room/sync/{roomId}")
//    @SendTo("/topic/room/sync/{roomId}")
//    @CrossOrigin
//    public byte[] handleSync(@DestinationVariable String roomId, @Payload byte[] payload) {
//        // Forwards the small, incremental Yjs updates to all clients in the room.
//        return payload;
//    }
//
//    /**
//     * Handles the initial request from a newly joined client.
//     * When a new client connects, it sends a message here to say "I need the current state."
//     * This message is then broadcast to all existing clients on a "request" topic.
//     */
//    @MessageMapping("/room/init/{roomId}")
//    @SendTo("/topic/room/requestInit/{roomId}")
//    @CrossOrigin
//    public String handleInitRequest(@DestinationVariable String roomId) {
//        // The payload can be empty. We just need to trigger the broadcast to ask
//        // existing clients for the full document state.
//        return "A new user needs the document state.";
//    }
//
//    /**
//     * Handles the response from an existing client that is providing the full document state.
//     * An existing client sends the complete document snapshot here, and this method
//     * broadcasts it to the "init" topic for the new user to consume.
//     */
//    @MessageMapping("/room/provideInit/{roomId}")
//    @SendTo("/topic/room/init/{roomId}")
//    @CrossOrigin
//    public byte[] handleProvideInit(@DestinationVariable String roomId, @Payload byte[] payload) {
//        // Forwards the complete Yjs document snapshot to the new client.
//        return payload;
//    }
//}
