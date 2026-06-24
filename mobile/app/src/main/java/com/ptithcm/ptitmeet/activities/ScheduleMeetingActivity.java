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
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.ChipGroup;
import com.ptithcm.ptitmeet.R;
import com.ptithcm.ptitmeet.viewmodel.ScheduleMeetingUiEvent;
import com.ptithcm.ptitmeet.viewmodel.ScheduleMeetingUiState;
import com.ptithcm.ptitmeet.viewmodel.ScheduleMeetingViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class ScheduleMeetingActivity extends AppCompatActivity {

    private EditText etTitle;
    private TextView tvDate, tvTime;
    private RadioGroup rgDuration;
    private Spinner spAccessType;
    private EditText etEmail;
    private ChipGroup layoutEmailsList;
    private CheckBox cbWaitingRoom, cbMuteAudio, cbMuteVideo, cbAllowChat, cbAllowScreenShare;
    private AppCompatButton btnSubmit;

    private ScheduleMeetingViewModel viewModel;

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
        viewModel = new ViewModelProvider(this).get(ScheduleMeetingViewModel.class);

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

        String[] accessTypes = {"TRUSTED (Cần phê duyệt)", "OPEN (Mở tự do)", "RESTRICTED (Chỉ khách mời)"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, accessTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAccessType.setAdapter(spinnerAdapter);
        spAccessType.setSelection(0);

        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        tvDate.setOnClickListener(v -> showDatePicker());
        tvTime.setOnClickListener(v -> showTimePicker());
        btnAddEmail.setOnClickListener(v -> addEmailToList());
        btnSubmit.setOnClickListener(v -> submitScheduleRequest());

        viewModel.getUiState().observe(this, this::applyState);
        viewModel.getUiEvent().observe(this, event -> {
            if (event == null) {
                return;
            }
            ScheduleMeetingUiEvent uiEvent = event.getContentIfNotHandled();
            if (uiEvent == null) {
                return;
            }
            handleEvent(uiEvent);
        });
    }

    private void showDatePicker() {
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
        Toast.makeText(this, "Đang xử lý yêu cầu...", Toast.LENGTH_SHORT).show();
        viewModel.submitScheduleRequest(
                etTitle.getText().toString().trim(),
                calendarStart,
                isDateSelected,
                isTimeSelected,
                spAccessType.getSelectedItemPosition(),
                rgDuration.getCheckedRadioButtonId(),
                cbWaitingRoom.isChecked(),
                cbMuteAudio.isChecked(),
                cbMuteVideo.isChecked(),
                cbAllowChat.isChecked(),
                cbAllowScreenShare.isChecked(),
                new ArrayList<>(participantEmails)
        );
    }

    private void applyState(ScheduleMeetingUiState state) {
        if (state == null) {
            return;
        }
        btnSubmit.setEnabled(!state.isSubmitting());
        btnSubmit.setText(state.isSubmitting() ? "Đang xử lý..." : "Xác nhận");
    }

    private void handleEvent(ScheduleMeetingUiEvent event) {
        switch (event.getType()) {
            case ScheduleMeetingUiEvent.SHOW_TOAST:
                Toast.makeText(this, event.getMessage(), Toast.LENGTH_SHORT).show();
                break;
            case ScheduleMeetingUiEvent.FINISH:
                finish();
                break;
            default:
                break;
        }
    }
}
