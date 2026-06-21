package com.ptithcm.ptitmeet.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.ptithcm.ptitmeet.api.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.models.MeetingAccessType;
import com.ptithcm.ptitmeet.repository.ScheduleMeetingRepository;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class ScheduleMeetingViewModel extends AndroidViewModel {

    private final ScheduleMeetingRepository repository;
    private final MutableLiveData<ScheduleMeetingUiState> uiState = new MutableLiveData<>(new ScheduleMeetingUiState());
    private final MutableLiveData<Event<ScheduleMeetingUiEvent>> uiEvent = new MutableLiveData<>();

    public ScheduleMeetingViewModel(@NonNull Application application) {
        super(application);
        this.repository = new ScheduleMeetingRepository(application);
    }

    public LiveData<ScheduleMeetingUiState> getUiState() {
        return uiState;
    }

    public LiveData<Event<ScheduleMeetingUiEvent>> getUiEvent() {
        return uiEvent;
    }

    public void submitScheduleRequest(
            String title,
            Calendar calendarStart,
            boolean isDateSelected,
            boolean isTimeSelected,
            int selectedAccessPos,
            int checkedDurationId,
            boolean waitingRoom,
            boolean muteAudio,
            boolean muteVideo,
            boolean allowChat,
            boolean allowScreenShare,
            List<String> participantEmails
    ) {
        if (!isDateSelected || !isTimeSelected) {
            uiEvent.setValue(new Event<>(ScheduleMeetingUiEvent.toast("Vui lòng chọn ngày và giờ bắt đầu cuộc họp")));
            return;
        }
        if (calendarStart.getTimeInMillis() < System.currentTimeMillis()) {
            uiEvent.setValue(new Event<>(ScheduleMeetingUiEvent.toast("Thời gian bắt đầu phải ở tương lai")));
            return;
        }

        updateState(true);

        String normalizedTitle = (title == null || title.trim().isEmpty())
                ? "Phòng họp của " + repository.getUserName()
                : title.trim();

        MeetingAccessType accessType = MeetingAccessType.TRUSTED;
        if (selectedAccessPos == 1) {
            accessType = MeetingAccessType.OPEN;
        } else if (selectedAccessPos == 2) {
            accessType = MeetingAccessType.RESTRICTED;
        }

        SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        sdfApi.setTimeZone(TimeZone.getDefault());
        String startTimeStr = sdfApi.format(calendarStart.getTime());

        int durationMin = 30;
        if (checkedDurationId == com.ptithcm.ptitmeet.R.id.rb15m) durationMin = 15;
        else if (checkedDurationId == com.ptithcm.ptitmeet.R.id.rb30m) durationMin = 30;
        else if (checkedDurationId == com.ptithcm.ptitmeet.R.id.rb45m) durationMin = 45;
        else if (checkedDurationId == com.ptithcm.ptitmeet.R.id.rb1h) durationMin = 60;
        else if (checkedDurationId == com.ptithcm.ptitmeet.R.id.rb2h) durationMin = 120;

        Calendar calendarEnd = (Calendar) calendarStart.clone();
        calendarEnd.add(Calendar.MINUTE, durationMin);
        String endTimeStr = sdfApi.format(calendarEnd.getTime());

        Map<String, Object> settingsMap = new HashMap<>();
        settingsMap.put("waitingRoom", waitingRoom);
        settingsMap.put("muteAudioOnEntry", muteAudio);
        settingsMap.put("muteVideoOnEntry", muteVideo);
        settingsMap.put("chatEnabled", allowChat);
        settingsMap.put("screenShareEnabled", allowScreenShare);

        CreateMeetingRequest request = new CreateMeetingRequest(normalizedTitle);
        request.setStartTime(startTimeStr);
        request.setEndTime(endTimeStr);
        request.setAccessType(accessType);
        request.setSettings(new Gson().toJson(settingsMap));
        if (participantEmails != null && !participantEmails.isEmpty()) {
            request.setParticipantEmails(participantEmails);
        }

        repository.scheduleMeeting(request, new ScheduleMeetingRepository.DataCallback<MeetingResponse>() {
            @Override
            public void onSuccess(MeetingResponse data) {
                updateState(false);
                uiEvent.postValue(new Event<>(ScheduleMeetingUiEvent.toast("Lên lịch họp thành công! Mã phòng: " + data.getMeetingCode())));
                uiEvent.postValue(new Event<>(ScheduleMeetingUiEvent.finishScreen()));
            }

            @Override
            public void onError(String message) {
                updateState(false);
                uiEvent.postValue(new Event<>(ScheduleMeetingUiEvent.toast(message)));
            }
        });
    }

    private void updateState(boolean submitting) {
        ScheduleMeetingUiState current = uiState.getValue();
        ScheduleMeetingUiState copy = current != null ? new ScheduleMeetingUiState(current) : new ScheduleMeetingUiState();
        copy.setSubmitting(submitting);
        uiState.postValue(copy);
    }
}
