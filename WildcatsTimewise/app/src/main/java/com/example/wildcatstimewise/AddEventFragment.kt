package com.example.wildcatstimewise

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.textfield.TextInputLayout
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class AddEventFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    private lateinit var dialogTitleTextView: TextView
    private lateinit var selectedDateTextView: TextView
    private lateinit var eventTitleEditText: EditText
    private lateinit var eventDescriptionEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var eventTypeSpinner: AutoCompleteTextView
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i(TAG, "POST_NOTIFICATIONS permission granted.")
                Toast.makeText(requireContext(), "Permission granted. Please try saving the event again to set reminders.", Toast.LENGTH_LONG).show()
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied.")
                Toast.makeText(requireContext(), "Notification permission denied. Reminders will not be set.", Toast.LENGTH_LONG).show()
            }
        }

    companion object {
        private const val TAG = "AddEventFragment"
        const val EVENT_START_OFFSET = 10000

        private const val ARG_YEAR = "year"; private const val ARG_MONTH = "month"; private const val ARG_DAY = "day"

        const val REQUEST_KEY = "addEventRequest"; const val RESULT_KEY_MODE = "result_mode";
        const val RESULT_KEY_ID = "result_event_id"
        const val RESULT_KEY_TITLE = "eventTitle"; const val RESULT_KEY_DESC = "eventDesc";
        const val RESULT_KEY_DATE_STR = "eventDateStr"; const val RESULT_KEY_TIME_STR = "eventTimeStr";
        const val RESULT_KEY_EVENT_TYPE = "eventType"

        fun newInstanceForAdd(year: Int, month: Int, day: Int): AddEventFragment {
            return AddEventFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_YEAR, year); putInt(ARG_MONTH, month); putInt(ARG_DAY, day)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val year = it.getInt(ARG_YEAR); val month = it.getInt(ARG_MONTH); val day = it.getInt(ARG_DAY)
            if (year != 0 && month != 0 && day != 0) {
                try { selectedDate = LocalDate.of(year, month, day) }
                catch (e: Exception) { Log.e(TAG, "Error creating date for add: ${e.message}"); dismissAllowingStateLoss() }
            } else { Log.e(TAG, "Invalid date args for add."); dismissAllowingStateLoss() }
        } ?: run { Log.e(TAG, "No args provided."); dismissAllowingStateLoss() }
    }

    private fun dismissWithError(message: String = "Error loading event data.") {
        if(context != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        dismissAllowingStateLoss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_add_event_fragment, container, false)

        dialogTitleTextView = view.findViewById(R.id.dialogTitleTextView)
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView)
        eventTitleEditText = view.findViewById(R.id.eventTitleEditText)
        eventDescriptionEditText = view.findViewById(R.id.eventDescriptionEditText)
        timeEditText = view.findViewById(R.id.timeEditText)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)
        eventTypeSpinner = view.findViewById(R.id.eventTypeSpinner)

        dialogTitleTextView.text = "Add New Event"
        saveButton.text = "Save"

        try {
            val eventTypes = resources.getStringArray(R.array.event_types)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, eventTypes)
            eventTypeSpinner.setAdapter(adapter)
            eventTypeSpinner.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) { try { eventTypeSpinner.showDropDown() } catch (e: Exception) { Log.e(TAG, "Error showing dropdown on focus", e) } } }
            eventTypeSpinner.setOnClickListener { try { eventTypeSpinner.showDropDown() } catch (e: Exception) { Log.e(TAG, "Error showing dropdown on click", e) } }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up event type AutoCompleteTextView adapter", e)
            Toast.makeText(context, "Could not load event types", Toast.LENGTH_SHORT).show()
        }

        updateDateDisplay(); updateTimeEditTextDisplay()

        val eventTimeInputLayout = view.findViewById<TextInputLayout>(R.id.eventTimeInputLayout)
        eventTimeInputLayout?.setEndIconOnClickListener {
            showTimePickerDialog()
        }
        timeEditText.setOnClickListener { showTimePickerDialog() }
        cancelButton.setOnClickListener { dismiss() }
        saveButton.setOnClickListener { handleSaveEvent() }
        return view
    }

    private fun showTimePickerDialog() {
        val initialTime = selectedTime ?: LocalTime.now()
        TimePickerDialog(requireContext(), this, initialTime.hour, initialTime.minute, false).show()
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        selectedTime = LocalTime.of(hourOfDay, minute);
        updateTimeEditTextDisplay()
        showTimeRemainingDialog()
    }

    private fun showTimeRemainingDialog() {
        if (selectedDate == null || selectedTime == null || context == null) {
            return
        }
        try {
            val eventDateTime = LocalDateTime.of(selectedDate!!, selectedTime!!)
            val now = LocalDateTime.now()

            val message: String
            if (eventDateTime.isAfter(now)) {
                val duration = Duration.between(now, eventDateTime)
                val days = duration.toDays()
                val hours = duration.toHours() % 24
                val minutes = duration.toMinutes() % 60

                message = when {
                    days > 0 -> "Time remaining: $days day(s), $hours hour(s), $minutes minute(s)."
                    hours > 0 -> "Time remaining: $hours hour(s), $minutes minute(s)."
                    minutes > 0 -> "Time remaining: $minutes minute(s)."
                    else -> "The event is starting now or very soon."
                }
            } else {
                message = "The selected time is in the past."
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Event Time")
                .setMessage(message)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating or showing time remaining dialog", e)
        }
    }

    private fun updateDateDisplay() {
        selectedDate?.let {
            val df = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            selectedDateTextView.text = "Date: ${it.format(df)}"
        } ?: run { selectedDateTextView.text = "Date not available"; Log.e(TAG, "selectedDate null") }
    }

    private fun updateTimeEditTextDisplay() {
        selectedTime?.let { val tf = DateTimeFormatter.ofPattern("hh:mm a"); timeEditText.setText(it.format(tf)) }
            ?: run { timeEditText.setText(""); }
    }

    private fun handleSaveEvent() {
        val title = eventTitleEditText.text.toString().trim()
        val description = eventDescriptionEditText.text.toString().trim()
        val eventType = eventTypeSpinner.text.toString().trim()

        val eventTypeInputLayout = view?.findViewById<TextInputLayout>(R.id.eventTypeInputLayout)
        val eventTitleInputLayout = view?.findViewById<TextInputLayout>(R.id.eventTitleInputLayout)

        if (eventType.isEmpty()) {
            eventTypeInputLayout?.error = "Please select or enter an event type."
            eventTypeSpinner.requestFocus(); return
        } else if (eventType.equals("Other...", ignoreCase = true)) {
            eventTypeInputLayout?.error = "Please enter your custom type or select another option."
            eventTypeSpinner.requestFocus(); return
        } else {
            eventTypeInputLayout?.error = null
        }

        if (title.isEmpty()) {
            eventTitleInputLayout?.error = "Title cannot be empty"
            eventTitleEditText.requestFocus(); return
        } else {
            eventTitleInputLayout?.error = null
        }
        if (selectedDate == null) { Toast.makeText(requireContext(), "Date missing.", Toast.LENGTH_SHORT).show(); return }

        val eventCheckDateTime = if (selectedTime != null) {
            LocalDateTime.of(selectedDate!!, selectedTime!!)
        } else {
            selectedDate!!.atStartOfDay()
        }
        val now = LocalDateTime.now()

        if (selectedTime != null && eventCheckDateTime.isBefore(now)) {
            Toast.makeText(requireContext(), "Cannot schedule event in the past.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedTime == null && selectedDate!!.isBefore(LocalDate.now())) {
            Toast.makeText(requireContext(), "Cannot schedule event for a past date.", Toast.LENGTH_SHORT).show()
            return
        }

        val result = Bundle().apply {
            putString(RESULT_KEY_MODE, "add")
            putString(RESULT_KEY_TITLE, title)
            putString(RESULT_KEY_DESC, description.ifEmpty { null })
            putString(RESULT_KEY_DATE_STR, selectedDate!!.toString())
            putString(RESULT_KEY_TIME_STR, selectedTime?.toString())
            putString(RESULT_KEY_EVENT_TYPE, eventType)
        }
        setFragmentResult(REQUEST_KEY, result)

        if (selectedDate != null && selectedTime != null) {
            val eventDateTime = LocalDateTime.of(selectedDate!!, selectedTime!!)
            if (eventDateTime.isAfter(now)) {
                checkPermissionsAndSchedule(title, description, eventDateTime)
            } else {
                Toast.makeText(requireContext(), "Event saved. Time is in the past, no reminders set.", Toast.LENGTH_LONG).show()
                dismiss()
            }
        } else {
            Toast.makeText(requireContext(), "Event saved (no time set).", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun checkPermissionsAndSchedule(title: String, description: String, eventDateTime: LocalDateTime) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ->
                    checkExactAlarmPermissionAndSchedule(title, description, eventDateTime)
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(requireContext(), "Notification permission needed to set reminders.", Toast.LENGTH_LONG).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else { checkExactAlarmPermissionAndSchedule(title, description, eventDateTime) }
    }

    private fun checkExactAlarmPermissionAndSchedule(title: String, description: String, eventDateTime: LocalDateTime) {
        val context = requireContext(); val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted.")
            Toast.makeText(context, "Permission to schedule exact alarms needed. Please grant in Settings.", Toast.LENGTH_LONG).show()
            try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))) }
            catch (e: Exception) { Log.e(TAG, "Could not open exact alarm settings: ${e.message}"); try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))) } catch (e2: Exception) { Log.e(TAG, "Could not open app details settings: ${e2.message}") } }
            return
        }
        scheduleAlarms(title, description, eventDateTime)
        dismiss()
    }

    private fun scheduleAlarms(title: String, description: String, eventDateTime: LocalDateTime) {
        val context = requireContext().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val eventZonedDateTime = ZonedDateTime.of(eventDateTime, ZoneId.systemDefault())
        val now = ZonedDateTime.now()
        var remindersScheduledCount = 0
        val baseRequestCode = eventDateTime.hashCode() + title.hashCode()


        val intervals = mapOf(
            Duration.ofHours(24) to 2400, Duration.ofHours(12) to 1200, Duration.ofHours(6) to 600,
            Duration.ofHours(3) to 300, Duration.ofMinutes(90) to 90, Duration.ofMinutes(30) to 30
        )

        intervals.forEach { (interval, intervalCode) ->
            val reminderTime = eventZonedDateTime.minus(interval)
            if (reminderTime.isAfter(now)) {
                val reminderRequestCode = baseRequestCode + intervalCode
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    action = NotificationReceiver.ACTION_SHOW_REMINDER_NOTIFICATION
                    putExtra(NotificationReceiver.EXTRA_TITLE, "$title (${formatDuration(interval)} before)")
                    putExtra(NotificationReceiver.EXTRA_MESSAGE, description.ifEmpty { "Reminder for $title" })
                    putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, baseRequestCode)
                    data = Uri.parse("wildcatstimewise://reminder/$reminderRequestCode")
                }
                scheduleExactAlarm(alarmManager, reminderTime.toInstant().toEpochMilli(), intent, reminderRequestCode)
                remindersScheduledCount++
            }
        }

        val eventStartTimeMillis = eventZonedDateTime.toInstant().toEpochMilli()
        if (eventStartTimeMillis > System.currentTimeMillis()) {
            val eventStartIntent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_EVENT_START_NOTIFICATION
                putExtra(NotificationReceiver.EXTRA_TITLE, title)
                putExtra(NotificationReceiver.EXTRA_MESSAGE, description.ifEmpty { "$title is starting now!" })
                putExtra(NotificationReceiver.EXTRA_NOTIFICATION_ID, baseRequestCode + EVENT_START_OFFSET)
                data = Uri.parse("wildcatstimewise://eventstart/${baseRequestCode + EVENT_START_OFFSET}")
            }
            scheduleExactAlarm(alarmManager, eventStartTimeMillis, eventStartIntent, baseRequestCode + EVENT_START_OFFSET)
            remindersScheduledCount++
        }

        if (remindersScheduledCount > 0) {
            Toast.makeText(context, "$remindersScheduledCount notification(s)/reminder(s) set.", Toast.LENGTH_SHORT).show()
        } else if (eventDateTime.isAfter(LocalDateTime.now())) {
            Toast.makeText(context, "Event time is too soon to set reminders or start notification.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Event saved.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, intent: Intent, requestCode: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext().applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Cannot schedule exact alarm (RC: $requestCode), permission possibly revoked.")
                return
            }
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            Log.i(TAG,"Alarm scheduled for RC: $requestCode at $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e(TAG,"SecurityException scheduling alarm (RC: $requestCode): ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG,"Error scheduling alarm (RC: $requestCode): ${e.message}")
        }
    }

    private fun formatDuration(duration: Duration): String {
        val days = duration.toDays()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60

        return when {
            days > 0 -> "$days day(s), $hours hr, $minutes min"
            hours > 0 -> "$hours hr, $minutes min"
            minutes > 0 -> "$minutes min"
            else -> "now or very soon"
        }
    }

    override fun onStart() {
        super.onStart()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}