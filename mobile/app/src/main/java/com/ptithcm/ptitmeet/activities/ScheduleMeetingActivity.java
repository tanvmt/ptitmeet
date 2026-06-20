package com.ptithcm.ptitmeet.activities;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.chip.ChipGroup;
import com.google.gson.Gson;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.api.SessionManager;
import com.ptithcm.ptitmeet.api.dto.common.ApiResponse;
import com.ptithcm.ptitmeet.api.dto.meeting.CreateMeetingRequest;
import com.ptithcm.ptitmeet.api.dto.meeting.MeetingResponse;
import com.ptithcm.ptitmeet.api.services.ApiService;
import com.ptithcm.ptitmeet.api.services.RetrofitClient;
import com.ptithcm.ptitmeet.models.MeetingAccessType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ScheduleMeetingActivity extends AppCompatActivity {

    private EditText etTitle;
    private TextView tvDate, tvTime;
    private RadioGroup rgDuration;
    private Spinner spAccessType;
    private EditText etEmail;
    private ChipGroup layoutEmailsList;
    private CheckBox cbWaitingRoom, cbMuteAudio, cbMuteVideo, cbAllowChat, cbAllowScreenShare;
    private AppCompatButton btnSubmit;

    private ApiService apiService;
    private SessionManager sessionManager;

    private Calendar calendarStart = Calendar.getInstance();
    private boolean isDateSelected = false;
    private boolean isTimeSelected = false;

    private List<String> participantEmails = new ArrayList<>();
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_meeting);

        apiService = RetrofitClient.getApiService(this);
        sessionManager = new SessionManager(this);

        // Bind views
        etTitle = findViewById(R.id.etTitle);
        tvDate = findViewById(R.id.tvDate);
        tvTime = findViewById(R.id.tvTime);
        rgDuration = findViewById(R.id.rgDuration);
        spAccessType = findViewById(R.id.spAccessType);
        etEmail = findViewById(R.id.etEmail);
        layoutEmailsList = findViewById(R.id.layoutEmailsList);
        cbWaitingRoom = findViewById(R.id.cbWaitingRoom);
        cbMuteAudio = findViewById(R.id.cbMuteAudio);
        cbMuteVideo = findViewById(R.id.cbMuteVideo);
        cbAllowChat = findViewById(R.id.cbAllowChat);
        cbAllowScreenShare = findViewById(R.id.cbAllowScreenShare);
        
        ImageView btnBack = findViewById(R.id.btnBack);
        AppCompatButton btnCancel = findViewById(R.id.btnCancel);
        btnSubmit = findViewById(R.id.btnSubmit);
        AppCompatButton btnAddEmail = findViewById(R.id.btnAddEmail);

        // Access Type Spinner Setup
        String[] accessTypes = {"TRUSTED (Cần phê duyệt)", "OPEN (Mở tự do)", "RESTRICTED (Chỉ khách mời)"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, accessTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAccessType.setAdapter(spinnerAdapter);
        spAccessType.setSelection(0); // Default TRUSTED

        // Listeners
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        tvDate.setOnClickListener(v -> showDatePicker());
        tvTime.setOnClickListener(v -> showTimePicker());
        btnAddEmail.setOnClickListener(v -> addEmailToList());
        btnSubmit.setOnClickListener(v -> submitScheduleRequest());
    }

    private void showDatePicker() {
        // Removed explicit Theme_AppCompat_Dialog style to fix compilation error
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    calendarStart.set(Calendar.YEAR, year);
                    calendarStart.set(Calendar.MONTH, month);
                    calendarStart.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    isDateSelected = true;

                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    tvDate.setText(sdf.format(calendarStart.getTime()));
                },
                calendarStart.get(Calendar.YEAR),
                calendarStart.get(Calendar.MONTH),
                calendarStart.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void showTimePicker() {
        // Removed explicit Theme_AppCompat_Dialog style to fix compilation error
        TimePickerDialog timePickerDialog = new TimePickerDialog(this,
                (view, hourOfDay, minute) -> {
                    calendarStart.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    calendarStart.set(Calendar.MINUTE, minute);
                    calendarStart.set(Calendar.SECOND, 0);
                    isTimeSelected = true;

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    tvTime.setText(sdf.format(calendarStart.getTime()));
                },
                calendarStart.get(Calendar.HOUR_OF_DAY),
                calendarStart.get(Calendar.MINUTE),
                true
        );
        timePickerDialog.show();
    }

    private void addEmailToList() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            Toast.makeText(this, "Email không đúng định dạng", Toast.LENGTH_SHORT).show();
            return;
        }

        if (participantEmails.contains(email)) {
            Toast.makeText(this, "Email này đã được thêm", Toast.LENGTH_SHORT).show();
            return;
        }

        participantEmails.add(email);
        etEmail.setText("");

        // Inflate dynamic item layout (Chip-like)
        View emailView = LayoutInflater.from(this).inflate(R.layout.item_email_chip, layoutEmailsList, false);
        TextView tvEmailChip = emailView.findViewById(R.id.tvEmail);
        ImageView btnRemove = emailView.findViewById(R.id.btnRemoveEmail);
        
        tvEmailChip.setText(email);
        btnRemove.setOnClickListener(v -> {
            participantEmails.remove(email);
            layoutEmailsList.removeView(emailView);
        });

        layoutEmailsList.addView(emailView);
    }

    private void submitScheduleRequest() {
        if (!isDateSelected || !isTimeSelected) {
            Toast.makeText(this, "Vui lòng chọn ngày và giờ bắt đầu cuộc họp", Toast.LENGTH_SHORT).show();
            return;
        }

        if (calendarStart.getTimeInMillis() < System.currentTimeMillis()) {
            Toast.makeText(this, "Thời gian bắt đầu phải ở tương lai", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmit.setEnabled(false);
        Toast.makeText(this, "Đang xử lý yêu cầu...", Toast.LENGTH_SHORT).show();

        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            title = "Phòng họp của " + sessionManager.getUserName();
        }

        // Determine Access Type
        MeetingAccessType accessType = MeetingAccessType.TRUSTED;
        int selectedAccessPos = spAccessType.getSelectedItemPosition();
        if (selectedAccessPos == 1) {
            accessType = MeetingAccessType.OPEN;
        } else if (selectedAccessPos == 2) {
            accessType = MeetingAccessType.RESTRICTED;
        }

        // ISO 8601 formatting for API
        SimpleDateFormat sdfApi = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        sdfApi.setTimeZone(TimeZone.getDefault());
        String startTimeStr = sdfApi.format(calendarStart.getTime());

        // Duration in minutes
        int durationMin = 30;
        int checkedId = rgDuration.getCheckedRadioButtonId();
        if (checkedId == R.id.rb15m) durationMin = 15;
        else if (checkedId == R.id.rb30m) durationMin = 30;
        else if (checkedId == R.id.rb45m) durationMin = 45;
        else if (checkedId == R.id.rb1h) durationMin = 60;
        else if (checkedId == R.id.rb2h) durationMin = 120;

        Calendar calendarEnd = (Calendar) calendarStart.clone();
        calendarEnd.add(Calendar.MINUTE, durationMin);
        String endTimeStr = sdfApi.format(calendarEnd.getTime());

        // Create Payload settings string
        Map<String, Object> settingsMap = new HashMap<>();
        settingsMap.put("waitingRoom", cbWaitingRoom.isChecked());
        settingsMap.put("muteAudioOnEntry", cbMuteAudio.isChecked());
        settingsMap.put("muteVideoOnEntry", cbMuteVideo.isChecked());
        settingsMap.put("chatEnabled", cbAllowChat.isChecked());
        settingsMap.put("screenShareEnabled", cbAllowScreenShare.isChecked());

        String settingsJson = new Gson().toJson(settingsMap);

        // Prepare Request DTO
        CreateMeetingRequest request = new CreateMeetingRequest(title);
        request.setStartTime(startTimeStr);
        request.setEndTime(endTimeStr);
        request.setAccessType(accessType);
        request.setSettings(settingsJson);
        if (!participantEmails.isEmpty()) {
            request.setParticipantEmails(participantEmails);
        }

        apiService.scheduleMeeting(request).enqueue(new Callback<ApiResponse<MeetingResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<MeetingResponse>> call, Response<ApiResponse<MeetingResponse>> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    MeetingResponse meeting = response.body().getData();
                    Toast.makeText(ScheduleMeetingActivity.this, "Lên lịch họp thành công! Mã phòng: " + meeting.getMeetingCode(), Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    String error = "Không thể lên lịch cuộc họp";
                    if (response.body() != null && response.body().getMessage() != null) {
                        error = response.body().getMessage();
                    }
                    Toast.makeText(ScheduleMeetingActivity.this, error, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<MeetingResponse>> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(ScheduleMeetingActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
