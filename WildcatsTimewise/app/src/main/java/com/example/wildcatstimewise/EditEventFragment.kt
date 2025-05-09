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
import java.time.format.DateTimeParseException
import kotlin.math.abs

class EditEventFragment : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    private lateinit var dialogTitleTextView: TextView
    private lateinit var selectedDateTextView: TextView
    private lateinit var eventTitleEditText: EditText
    private lateinit var eventDescriptionEditText: EditText
    private lateinit var timeEditText: EditText
    private lateinit var eventTypeSpinner: AutoCompleteTextView
    private lateinit var updateButton: Button
    private lateinit var cancelButton: Button

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var existingEventId: String? = null
    private var existingEventType: String? = null

    private var originalEventTitleForNotif: String? = null
    private var originalEventDateTimeForNotif: LocalDateTime? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.i(TAG, "POST_NOTIFICATIONS permission granted for edit.")
                Toast.makeText(requireContext(), "Permission granted. Please save again to update reminders.", Toast.LENGTH_LONG).show()
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied for edit.")
                Toast.makeText(requireContext(), "Notification permission denied. Reminders will not be updated.", Toast.LENGTH_LONG).show()
            }
        }

    companion object {
        private const val TAG = "EditEventFragment"
        private const val EVENT_START_OFFSET = AddEventFragment.EVENT_START_OFFSET

        private const val ARG_EVENT_ID = "event_id"
        private const val ARG_EVENT_TITLE = "event_title"
        private const val ARG_EVENT_DESC = "event_desc"
        private const val ARG_EVENT_DATE_STR = "event_date_str"
        private const val ARG_EVENT_TIME_STR = "event_time_str"
        private const val ARG_EVENT_TYPE = "event_type"

        const val REQUEST_KEY = AddEventFragment.REQUEST_KEY
        const val RESULT_KEY_MODE = AddEventFragment.RESULT_KEY_MODE
        const val RESULT_KEY_ID = AddEventFragment.RESULT_KEY_ID
        const val RESULT_KEY_TITLE = AddEventFragment.RESULT_KEY_TITLE
        const val RESULT_KEY_DESC = AddEventFragment.RESULT_KEY_DESC
        const val RESULT_KEY_DATE_STR = AddEventFragment.RESULT_KEY_DATE_STR
        const val RESULT_KEY_TIME_STR = AddEventFragment.RESULT_KEY_TIME_STR
        const val RESULT_KEY_EVENT_TYPE = AddEventFragment.RESULT_KEY_EVENT_TYPE

        fun newInstance(event: EventInfo): EditEventFragment {
            requireNotNull(event.id) { "Cannot edit event without an ID" }
            require(event.isUserEvent) { "Cannot edit non-user events" }

            return EditEventFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_EVENT_ID, event.id)
                    putString(ARG_EVENT_TITLE, event.name)
                    putString(ARG_EVENT_DESC, event.description ?: "")
                    putString(ARG_EVENT_DATE_STR, event.date.toString())
                    putString(ARG_EVENT_TIME_STR, event.time?.toString())
                    putString(ARG_EVENT_TYPE, event.type)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            existingEventId = it.getString(ARG_EVENT_ID)
            originalEventTitleForNotif = it.getString(ARG_EVENT_TITLE)
            existingEventType = it.getString(ARG_EVENT_TYPE)
            val dateStr = it.getString(ARG_EVENT_DATE_STR)
            val timeStr = it.getString(ARG_EVENT_TIME_STR)

            try {
                val originalDate = dateStr?.let { LocalDate.parse(it) }
                val originalTime = timeStr?.let { LocalTime.parse(it) }

                selectedDate = originalDate
                selectedTime = originalTime

                if (originalDate != null && originalEventTitleForNotif != null) {
                    originalEventDateTimeForNotif = if (originalTime != null) {
                        LocalDateTime.of(originalDate, originalTime)
                    } else {
                        null
                    }
                }
            } catch (e: DateTimeParseException) {
                Log.e(TAG, "Error parsing original date/time for edit: ${e.message}")
                dismissWithError()
            }
            if (selectedDate == null || existingEventId.isNullOrEmpty() || originalEventTitleForNotif == null) {
                Log.e(TAG, "Missing essential original data for edit mode.")
                dismissWithError()
            }
        } ?: run {
            Log.e(TAG, "No arguments provided for edit.")
            dismissWithError()
        }
    }

    private fun dismissWithError(message: String = "Error loading event data.") {
        if (context != null) Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        dismissAllowingStateLoss()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.activity_edit_event_fragment, container, false)

        dialogTitleTextView = view.findViewById(R.id.dialogTitleTextView)
        selectedDateTextView = view.findViewById(R.id.selectedDateTextView)
        eventTitleEditText = view.findViewById(R.id.eventTitleEditText)
        eventDescriptionEditText = view.findViewById(R.id.eventDescriptionEditText)
        timeEditText = view.findViewById(R.id.timeEditText)
        eventTypeSpinner = view.findViewById(R.id.eventTypeSpinner)
        updateButton = view.findViewById(R.id.updateButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        dialogTitleTextView.text = "Edit Event"

        arguments?.let {
            eventTitleEditText.setText(it.getString(ARG_EVENT_TITLE, ""))
            eventDescriptionEditText.setText(it.getString(ARG_EVENT_DESC, ""))
        }

        updateDateDisplay()
        updateTimeEditTextDisplay()
        setupEventTypeAutoComplete()

        val eventTimeInputLayout = view.findViewById<TextInputLayout>(R.id.eventTimeInputLayout)
        eventTimeInputLayout?.setEndIconOnClickListener {
            showTimePickerDialog()
        }
        timeEditText.setOnClickListener { showTimePickerDialog() }
        cancelButton.setOnClickListener { dismiss() }
        updateButton.setOnClickListener { handleUpdateEvent() }
        return view
    }

    private fun setupEventTypeAutoComplete() {
        try {
            val eventTypes = resources.getStringArray(R.array.event_types)
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, eventTypes)
            eventTypeSpinner.setAdapter(adapter)
            existingEventType?.let {
                eventTypeSpinner.setText(it, false)
            }
            eventTypeSpinner.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) { try { eventTypeSpinner.showDropDown() } catch (e: Exception) { Log.e(TAG, "Error showing dropdown on focus", e) } } }
            eventTypeSpinner.setOnClickListener { try { eventTypeSpinner.showDropDown() } catch (e: Exception) { Log.e(TAG, "Error showing dropdown on click", e) } }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up event type AutoCompleteTextView adapter", e)
            Toast.makeText(context, "Could not load event types", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showTimePickerDialog() {
        val initialTime = selectedTime ?: LocalTime.now()
        TimePickerDialog(requireContext(), this, initialTime.hour, initialTime.minute, false).show()
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        selectedTime = LocalTime.of(hourOfDay, minute)
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
            val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
            selectedDateTextView.text = "Date: ${it.format(dateFormatter)}"
        } ?: run {
            selectedDateTextView.text = "Date not available"
            Log.e(TAG, "selectedDate null in updateDateDisplay")
        }
    }

    private fun updateTimeEditTextDisplay() {
        selectedTime?.let {
            val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
            timeEditText.setText(it.format(timeFormatter))
        } ?: run {
            timeEditText.setText("")
        }
    }

    private fun handleUpdateEvent() {
        val newTitle = eventTitleEditText.text.toString().trim()
        val newDescription = eventDescriptionEditText.text.toString().trim()
        val newSelectedType = eventTypeSpinner.text.toString().trim()

        var isValid = true
        val eventTitleInputLayout = view?.findViewById<TextInputLayout>(R.id.eventTitleInputLayout)
        val eventTypeInputLayout = view?.findViewById<TextInputLayout>(R.id.eventTypeInputLayout)

        if (newSelectedType.isEmpty()) {
            eventTypeInputLayout?.error = "Please select or enter an event type."
            eventTypeSpinner.requestFocus(); isValid = false
        } else if (newSelectedType.equals("Other...", ignoreCase = true)) {
            eventTypeInputLayout?.error = "Please enter your custom type or select another option."
            eventTypeSpinner.requestFocus(); isValid = false
        } else {
            eventTypeInputLayout?.error = null
        }

        if (newTitle.isEmpty()) {
            eventTitleInputLayout?.error = "Title cannot be empty"
            eventTitleEditText.requestFocus(); isValid = false
        } else {
            eventTitleInputLayout?.error = null
        }

        if (!isValid) return

        if (selectedDate == null || existingEventId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Cannot update - essential data missing.", Toast.LENGTH_SHORT).show()
            return
        }

        val newEventCheckDateTime = if (selectedTime != null) {
            LocalDateTime.of(selectedDate!!, selectedTime!!)
        } else {
            selectedDate!!.atStartOfDay()
        }
        val now = LocalDateTime.now()

        if (selectedTime != null && newEventCheckDateTime.isBefore(now)) {
            Toast.makeText(requireContext(), "Cannot set event time in the past if notifications are desired.", Toast.LENGTH_SHORT).show()
        }
        if (selectedTime == null && selectedDate!!.isBefore(LocalDate.now())) {
            // Allow saving past events without time
        }

        if (originalEventTitleForNotif != null && originalEventDateTimeForNotif != null) {
            cancelOldAlarms(originalEventTitleForNotif!!, originalEventDateTimeForNotif!!)
        } else {
            Log.w(TAG, "Could not cancel all old alarms: original title or datetime for notification missing/null.")
        }

        val result = Bundle().apply {
            putString(RESULT_KEY_MODE, "edit")
            putString(RESULT_KEY_ID, existingEventId)
            putString(RESULT_KEY_TITLE, newTitle)
            putString(RESULT_KEY_DESC, newDescription.ifEmpty { null })
            putString(RESULT_KEY_DATE_STR, selectedDate!!.toString())
            putString(RESULT_KEY_TIME_STR, selectedTime?.toString())
            putString(RESULT_KEY_EVENT_TYPE, newSelectedType)
        }
        setFragmentResult(REQUEST_KEY, result)

        if (selectedDate != null && selectedTime != null) {
            val newEventDateTime = LocalDateTime.of(selectedDate!!, selectedTime!!)
            if (newEventDateTime.isAfter(LocalDateTime.now())) {
                checkPermissionsAndSchedule(newTitle, newDescription, newEventDateTime)
            } else {
                Toast.makeText(requireContext(), "Event updated. New time is in the past, no new reminders set.", Toast.LENGTH_LONG).show()
                dismiss()
            }
        } else {
            Toast.makeText(requireContext(), "Event updated. No time set for reminders.", Toast.LENGTH_SHORT).show()
            dismiss()
        }
    }

    private fun cancelOldAlarms(titleForCancellation: String, dateTimeForCancellation: LocalDateTime) {
        if (context == null) {
            Log.e(TAG, "Context is null, cannot cancel alarms.")
            return
        }
        Log.d(TAG, "Attempting to cancel old alarms for: $titleForCancellation at $dateTimeForCancellation")
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intervals = mapOf(
            Duration.ofHours(24) to 2400, Duration.ofHours(12) to 1200, Duration.ofHours(6) to 600,
            Duration.ofHours(3) to 300, Duration.ofMinutes(90) to 90, Duration.ofMinutes(30) to 30
        )
        val originalBaseRequestCode = dateTimeForCancellation.hashCode() + titleForCancellation.hashCode()

        intervals.forEach { (_, intervalCode) ->
            val reminderRequestCode = originalBaseRequestCode + intervalCode
            val intent = Intent(requireContext(), NotificationReceiver::class.java).apply {
                action = NotificationReceiver.ACTION_SHOW_REMINDER_NOTIFICATION
                data = Uri.parse("wildcatstimewise://reminder/$reminderRequestCode")
            }
            cancelSpecificAlarm(alarmManager, intent, reminderRequestCode, "reminder")
        }

        val eventStartRequestCode = originalBaseRequestCode + EVENT_START_OFFSET
        val eventStartIntent = Intent(requireContext(), NotificationReceiver::class.java).apply {
            action = NotificationReceiver.ACTION_EVENT_START_NOTIFICATION
            data = Uri.parse("wildcatstimewise://eventstart/$eventStartRequestCode")
        }
        cancelSpecificAlarm(alarmManager, eventStartIntent, eventStartRequestCode, "event start")
    }

    private fun cancelSpecificAlarm(alarmManager: AlarmManager, intent: Intent, requestCode: Int, alarmType: String) {
        if (context == null) return
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext().applicationContext,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.i(TAG, "Cancelled old $alarmType alarm with RC: $requestCode")
        } else {
            Log.d(TAG, "No old $alarmType alarm found to cancel with RC: $requestCode")
        }
    }

    private fun checkPermissionsAndSchedule(title: String, description: String, eventDateTime: LocalDateTime) {
        if (context == null) { Log.e(TAG, "Context null in checkPermissionsAndSchedule"); return }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ->
                    checkExactAlarmPermissionAndSchedule(title, description, eventDateTime)
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    Toast.makeText(requireContext(), "Notification permission needed for reminders.", Toast.LENGTH_LONG).show()
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            checkExactAlarmPermissionAndSchedule(title, description, eventDateTime)
        }
    }

    private fun checkExactAlarmPermissionAndSchedule(title: String, description: String, eventDateTime: LocalDateTime) {
        if (context == null) { Log.e(TAG, "Context null in checkExactAlarmPermissionAndSchedule"); return }
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted for edit.")
            Toast.makeText(requireContext(), "Permission to schedule exact alarms needed. Grant in Settings.", Toast.LENGTH_LONG).show()
            try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${requireContext().packageName}"))) }
            catch (e: Exception) { Log.e(TAG, "Could not open exact alarm settings: ${e.message}"); try { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${requireContext().packageName}"))) } catch (e2: Exception) { Log.e(TAG, "Could not open app details settings: ${e2.message}") } }
            return
        }
        scheduleAlarms(title, description, eventDateTime)
        dismiss()
    }

    private fun scheduleAlarms(title: String, description: String, eventDateTime: LocalDateTime) {
        if (context == null) { Log.e(TAG, "Context null in scheduleAlarms"); return }
        val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
                val intent = Intent(requireContext().applicationContext, NotificationReceiver::class.java).apply {
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
            val eventStartIntent = Intent(requireContext().applicationContext, NotificationReceiver::class.java).apply {
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
            Toast.makeText(requireContext().applicationContext, "$remindersScheduledCount notification(s)/reminder(s) set for updated event.", Toast.LENGTH_SHORT).show()
        } else if (eventDateTime.isAfter(LocalDateTime.now())) {
            Toast.makeText(requireContext().applicationContext, "Updated event time is too soon to set new reminders or start notification.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scheduleExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, intent: Intent, requestCode: Int) {
        if (context == null) return
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