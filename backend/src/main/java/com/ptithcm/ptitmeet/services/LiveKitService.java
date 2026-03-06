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
import livekit.LivekitEgress.S3Upload;
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

    @Value("${livekit.api.ws-url}")
    private String livekitWsUrl;

    @Value("${livekit.api.key}")
    private String livekitApiKey;

    @Value("${livekit.api.secret}")
    private String livekitApiSecret;

    // Cloudflare R2 (S3-compatible) config
    @Value("${s3.endpoint}")
    private String s3Endpoint;

    @Value("${s3.access-key}")
    private String s3AccessKey;

    @Value("${s3.secret-key}")
    private String s3SecretKey;

    @Value("${s3.bucket}")
    private String s3Bucket;

    @Value("${s3.region}")
    private String s3Region;

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
        // 1. Cấu hình upload lên Cloudflare R2 (S3-compatible)
        String fileName = "recordings/record_" + roomName + "_" + System.currentTimeMillis() + ".mp4";

        S3Upload s3Upload = S3Upload.newBuilder()
                .setAccessKey(s3AccessKey)
                .setSecret(s3SecretKey)
                .setBucket(s3Bucket)
                .setRegion(s3Region)
                .setEndpoint(s3Endpoint)
                .setForcePathStyle(true)
                .build();

        EncodedFileOutput fileOutput = EncodedFileOutput.newBuilder()
                .setFilepath(fileName)
                .setS3(s3Upload)
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
                recording.setFileUrl(fileName);

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

    /**
     * Cập nhật recording khi egress hoàn tất (gọi từ webhook).
     * Lưu fileUrl và đổi status thành COMPLETED.
     */
    @Transactional
    public void updateRecordingCompleted(String egressId, String fileUrl) {
        MeetingRecording recording = recordingRepository.findByEgressId(egressId)
                .orElse(null);
        if (recording != null) {
            recording.setFileUrl(fileUrl);
            recording.setStatus("COMPLETED");
            recordingRepository.save(recording);
            log.info("Recording COMPLETED - egressId: {}, fileUrl: {}", egressId, fileUrl);
        } else {
            log.warn("Không tìm thấy recording với egressId: {}", egressId);
        }
    }

    /**
     * Cập nhật recording khi egress thất bại (gọi từ webhook).
     */
    @Transactional
    public void updateRecordingFailed(String egressId) {
        MeetingRecording recording = recordingRepository.findByEgressId(egressId)
                .orElse(null);
        if (recording != null) {
            recording.setStatus("FAILED");
            recordingRepository.save(recording);
            log.warn("Recording FAILED - egressId: {}", egressId);
        }
    }

    /**
     * Lấy thông tin recording theo egressId.
     */
    public MeetingRecording getRecordingByEgressId(String egressId) {
        return recordingRepository.findByEgressId(egressId)
                .orElseThrow(() -> new RuntimeException(
                        "Không tìm thấy recording với egressId: " + egressId));
    }

    public String generateJoinToken(String roomName, String participantName, String participantId) {
        AccessToken token = new AccessToken(livekitApiKey, livekitApiSecret);

        token.setName(participantName);

        token.setIdentity(participantId);

        token.addGrants(new RoomJoin(true), new RoomName(roomName));

        return token.toJwt();
    }

}
