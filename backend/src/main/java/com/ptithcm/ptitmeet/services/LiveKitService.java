package com.ptithcm.ptitmeet.services;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ptithcm.ptitmeet.entity.mysql.MeetingRecording;
import com.ptithcm.ptitmeet.repositories.MeetingRecordingRepository;

import io.livekit.server.AccessToken;
import io.livekit.server.EgressServiceClient;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import jakarta.annotation.PostConstruct;
import livekit.LivekitEgress.EgressInfo;
import livekit.LivekitEgress.EncodedFileOutput;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

@Service
@Slf4j
@Getter
public class LiveKitService {

    @Value("${livekit.api.url}")
    private String livekitUrl;

    @Value("${livekit.api.key}")
    private String livekitApiKey;

    @Value("${livekit.api.secret}")
    private String livekitApiSecret;

    
    @Autowired
    private MeetingRecordingRepository recordingRepository;

    private EgressServiceClient egressClient;

    // Khởi tạo Client 1 lần duy nhất để tối ưu hiệu suất
    @PostConstruct
    public void init() {
        this.egressClient = EgressServiceClient.createClient(livekitUrl, livekitApiKey, livekitApiSecret);
    }

    @Transactional
    public MeetingRecording startRoomRecording(String roomName) {
        // 1. Cấu hình file đầu ra cho LiveKit
        String fileName = "record_" + roomName + "_" + System.currentTimeMillis() + ".mp4";
        EncodedFileOutput fileOutput = EncodedFileOutput.newBuilder()
                .setFilepath("/out/" + fileName)
                .build();

        Call<EgressInfo> call = egressClient.startRoomCompositeEgress(roomName, fileOutput);

        try {
            Response<EgressInfo> response = call.execute();

            if (response.isSuccessful() && response.body().getEgressId() != null) {
                String egressId = response.body().getEgressId();
                log.info("Bắt đầu ghi hình thành công, EgressId: {}", egressId);

                MeetingRecording recording = new MeetingRecording();
                recording.setRoomName(roomName);
                recording.setEgressId(egressId);
                recording.setStatus("RECORDING");
                recording.setCreatedAt(LocalDateTime.now());

                return recordingRepository.save(recording);
            } else {
                log.error("LiveKit Server báo lỗi: {}",
                        response.errorBody() != null ? response.errorBody().string() : "Unknown");
                throw new RuntimeException("Không thể khởi tạo ghi hình từ LiveKit");
            }
        } catch (IOException e) {
            log.error("Lỗi kết nối mạng đến LiveKit Server tại phòng: {}", roomName, e);
            throw new RuntimeException("Lỗi kết nối đến LiveKit Server");
        }
    }

    @Transactional
    public MeetingRecording stopRecording(String egressId) {

        boolean isStopSuccess = false;
        try {
            Response<EgressInfo> response = egressClient.stopEgress(egressId).execute();
            if (response.isSuccessful()) {
                isStopSuccess = true;
                log.info("Đã gửi lệnh dừng ghi hình cho EgressId: {}", egressId);
            }
        } catch (IOException e) {
            log.error("Lỗi khi gửi lệnh dừng phòng họp tại egressId: {}", egressId, e);
        }

        MeetingRecording recording = recordingRepository.findByEgressId(egressId)
                .orElseThrow(() -> new RuntimeException(
                "Không tìm thấy thông tin ghi hình trong Database với ID: " + egressId));

        if (isStopSuccess) {
            recording.setStatus("STOPPING");
        } else {
            recording.setStatus("FAILED");
        }

        return recordingRepository.save(recording);
    }

    public String generateJoinToken(String roomName, String participantName, String participantId) {
        AccessToken token = new AccessToken(livekitApiKey, livekitApiSecret);

        token.setName(participantName);

        token.setIdentity(participantId);

        token.addGrants(new RoomJoin(true), new RoomName(roomName));

        return token.toJwt();
    }

}
