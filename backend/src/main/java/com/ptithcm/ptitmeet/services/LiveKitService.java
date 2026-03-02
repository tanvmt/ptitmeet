package com.ptithcm.ptitmeet.services;

import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.mysql.User;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LiveKitService {

    @Value("${livekit.api.key}")
    private String livekitApiKey;

    @Value("${livekit.api.secret}")
    private String livekitApiSecret;

    @Getter
    @Value("${livekit.api.url}")
    private String livekitUrl;


    public String generateJoinToken(String roomName, String participantName, String participantId) {
        AccessToken token = new AccessToken(livekitApiKey, livekitApiSecret);

        token.setName(participantName);

        token.setIdentity(participantId);

        token.addGrants(new RoomJoin(true), new RoomName(roomName));

        return token.toJwt();
    }

}