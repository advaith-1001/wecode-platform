package com.adv.wecode_springboot_backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Arrays;
import java.util.Base64;

@Controller
public class RoomController {

    SimpMessagingTemplate messagingTemplate;


    public RoomController(SimpMessagingTemplate messagingTemplate) {

        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/room/sync/{roomId}")
    public void syncRoom(@DestinationVariable String roomId, @Payload String base64Update) {
        byte[] update = Base64.getDecoder().decode(base64Update);
        System.out.println("roomId: " + roomId);
        System.out.println("update: " + Arrays.toString(update));
        messagingTemplate.convertAndSend("/topic/room/sync/" + roomId, base64Update); // forward as-is
    }

    @MessageMapping("/room/init/{roomId}")
    public void requestInit(@DestinationVariable String roomId) {
        // Ask all existing clients for their latest snapshot
        messagingTemplate.convertAndSend("/topic/room/requestInit/" + roomId, "");
    }

    @MessageMapping("/room/provideInit/{roomId}")
    public void provideInit(@DestinationVariable String roomId, @Payload String base64Snapshot) {
        // Send snapshot to all clients (or just the new one, if tracked)
        messagingTemplate.convertAndSend("/topic/room/init/" + roomId, base64Snapshot);
    }
}

