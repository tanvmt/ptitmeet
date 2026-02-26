package com.ptithcm.ptitmeet.services;

import com.ptithcm.ptitmeet.entity.mysql.Meeting;
import com.ptithcm.ptitmeet.entity.mysql.User;
import org.springframework.stereotype.Service;

@Service
public class LiveKitService {

    public String generateToken(User user, Meeting meeting) {
        return "mock_token_" + user.getEmail() + "_" + meeting.getMeetingCode();
    }
    
    public String getLiveKitUrl() {
        return "ws://localhost:7880";
    }
}