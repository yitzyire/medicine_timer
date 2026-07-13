package com.medicinetimer.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedicineTimerTheme {
                MedicineTimerApp()
            }
        }
    }
}

private data class MedicineUi(
    val id: Int,
    val name: String,
    val expiry: String,
    val color: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val reminders: List<DoseReminder>,
)

private data class DoseReminder(
    val time: String,
    val takenAt: LocalDateTime? = null,
)

private val completedTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
private val completedDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val expiryDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy")
private val reminderTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")

private val medicineColors = listOf(
    Color(0xFFF2B84B),
    Color(0xFF2F6B5F),
    Color(0xFF7B61FF),
    Color(0xFF3D8BFF),
    Color(0xFFE56B6F),
)

private val medicineBackgroundColors = listOf(
    Color(0xFFFFF4DD),
    Color(0xFFEAF5F1),
    Color(0xFFF0ECFF),
    Color(0xFFEAF3FF),
    Color(0xFFFFEEEE),
)

private val medicineSurfaceColors = listOf(
    Color(0xFFFFFAF0),
    Color(0xFFF6FBF9),
    Color(0xFFF8F6FF),
    Color(0xFFF5FAFF),
    Color(0xFFFFF8F8),
)

private val starterMedicines = listOf(
    MedicineUi(
        id = 1,
        name = "Vitamin D",
        expiry = "Expires 12 Aug 2026",
        color = Color(0xFFF2B84B),
        backgroundColor = Color(0xFFFFF4DD),
        surfaceColor = Color(0xFFFFFAF0),
        reminders = listOf(DoseReminder("8:00 AM")),
    ),
    MedicineUi(
        id = 2,
        name = "Blood Pressure",
        expiry = "Expires 04 Nov 2026",
        color = Color(0xFF2F6B5F),
        backgroundColor = Color(0xFFEAF5F1),
        surfaceColor = Color(0xFFF6FBF9),
        reminders = listOf(
            DoseReminder("8:00 AM", takenAt = LocalDateTime.of(2026, 7, 12, 8, 7)),
            DoseReminder("8:00 PM"),
        ),
    ),
    MedicineUi(
        id = 3,
        name = "Evening Tablet",
        expiry = "Expires 18 Jan 2027",
        color = Color(0xFF7B61FF),
        backgroundColor = Color(0xFFF0ECFF),
        surfaceColor = Color(0xFFF8F6FF),
        reminders = listOf(DoseReminder("4:00 PM"), DoseReminder("12:00 AM")),
    ),
)

@Composable
private fun MedicineTimerApp() {
    val context = LocalContext.current
    var showLoading by remember { mutableStateOf(true) }
    var page by remember { mutableIntStateOf(0) }
    var selectedMedicineId by remember { mutableStateOf<Int?>(null) }
    var medicines by remember { mutableStateOf(loadMedicines(context)) }
    var medicineBeingEdited by remember { mutableStateOf<MedicineUi?>(null) }
    var medicineAddingReminderId by remember { mutableStateOf<Int?>(null) }
    var showAddMedicineDialog by remember { mutableStateOf(false) }
    var medicinePendingDelete by remember { mutableStateOf<MedicineUi?>(null) }
    var tabSwipeOffset by remember { mutableStateOf(0f) }

    val selectedMedicine = medicines.firstOrNull { it.id == selectedMedicineId }

    LaunchedEffect(Unit) {
        delay(650)
        showLoading = false
    }

    LaunchedEffect(medicines) {
        saveMedicines(context, medicines)
    }

    if (showLoading) {
        AppLoadingScreen()
        return
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFFF7FAF8),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Header(
                title = selectedMedicine?.name ?: "Medicine Timer",
                page = page,
                showBack = selectedMedicine != null,
                onBack = { selectedMedicineId = null },
                onPageChange = {
                    selectedMedicineId = null
                    page = it
                },
            )
            Spacer(modifier = Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (selectedMedicine == null) {
                            Modifier.pointerInput(page) {
                                detectHorizontalDragGestures(
                                    onDragStart = { tabSwipeOffset = 0f },
                                    onHorizontalDrag = { _, dragAmount ->
                                        tabSwipeOffset += dragAmount
                                    },
                                    onDragEnd = {
                                        when {
                                            tabSwipeOffset <= -80f -> page = (page + 1).coerceAtMost(2)
                                            tabSwipeOffset >= 80f -> page = (page - 1).coerceAtLeast(0)
                                        }
                                        tabSwipeOffset = 0f
                                    },
                                    onDragCancel = { tabSwipeOffset = 0f },
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            ) {
                when {
                    selectedMedicine != null -> MedicineDetailPage(
                        medicine = selectedMedicine,
                        onEditMedicine = { medicineBeingEdited = selectedMedicine },
                        onDeleteMedicine = { medicinePendingDelete = selectedMedicine },
                        onAddReminder = { medicineAddingReminderId = selectedMedicine.id },
                        onMarkTaken = { time ->
                            val completedAt = LocalDateTime.now()
                            medicines = medicines.map { medicine ->
                                if (medicine.id == selectedMedicine.id) {
                                    medicine.copy(
                                        reminders = medicine.reminders.map { reminder ->
                                            if (reminder.time == time && reminder.takenAt == null) {
                                                reminder.copy(takenAt = completedAt)
                                            } else {
                                                reminder
                                            }
                                        },
                                    )
                                } else {
                                    medicine
                                }
                            }
                        },
                        onUndoTaken = { time ->
                            medicines = medicines.undoTakenDose(selectedMedicine.id, time)
                        },
                    )

                    page == 0 -> MedicinesPage(
                        medicines = medicines,
                        onMedicineClick = { selectedMedicineId = it.id },
                        onAddMedicine = { showAddMedicineDialog = true },
                        onEditMedicine = { medicineBeingEdited = it },
                        onDeleteMedicine = { medicinePendingDelete = it },
                        onMarkTaken = { medicineId, time ->
                            val completedAt = LocalDateTime.now()
                            medicines = medicines.map { medicine ->
                                if (medicine.id == medicineId) {
                                    medicine.copy(
                                        reminders = medicine.reminders.map { reminder ->
                                            if (reminder.time == time && reminder.takenAt == null) {
                                                reminder.copy(takenAt = completedAt)
                                            } else {
                                                reminder
                                            }
                                        },
                                    )
                                } else {
                                    medicine
                                }
                            }
                        },
                    )

                    page == 1 -> CompletedPage(
                        medicines = medicines,
                        onUndoTaken = { medicineId, time ->
                            medicines = medicines.undoTakenDose(medicineId, time)
                        },
                    )

                    else -> CalendarPage(
                        medicines = medicines,
                    )
                }
            }
        }
    }

    medicineAddingReminderId?.let { medicineId ->
        val medicine = medicines.firstOrNull { it.id == medicineId }
        if (medicine == null) {
            medicineAddingReminderId = null
        } else {
            AddReminderDialog(
                medicine = medicine,
                onDismiss = { medicineAddingReminderId = null },
                onConfirm = { time ->
                    medicines = medicines.map { medicine ->
                        if (medicine.id == medicineId) {
                            medicine.copy(reminders = medicine.reminders.addReminder(time))
                        } else {
                            medicine
                        }
                    }
                    medicineAddingReminderId = null
                },
            )
        }
    }

    if (showAddMedicineDialog) {
        val newId = (medicines.maxOfOrNull { it.id } ?: 0) + 1
        MedicineEditorDialog(
            title = "Add medicine",
            initialName = "",
            initialExpiryDate = LocalDate.now().plusMonths(6),
            accentColor = medicineColors[(newId - 1) % medicineColors.size],
            containerColor = medicineBackgroundColors[(newId - 1) % medicineBackgroundColors.size],
            confirmLabel = "Add",
            onDismiss = { showAddMedicineDialog = false },
            onConfirm = { name, expiryDate ->
                medicines = medicines + MedicineUi(
                    id = newId,
                    name = name,
                    expiry = expiryDate.toExpiryLabel(),
                    color = medicineColors[(newId - 1) % medicineColors.size],
                    backgroundColor = medicineBackgroundColors[(newId - 1) % medicineBackgroundColors.size],
                    surfaceColor = medicineSurfaceColors[(newId - 1) % medicineSurfaceColors.size],
                    reminders = emptyList(),
                )
                showAddMedicineDialog = false
            },
        )
    }

    medicineBeingEdited?.let { editingMedicine ->
        MedicineEditorDialog(
            title = "Edit medicine",
            initialName = editingMedicine.name,
            initialExpiryDate = editingMedicine.expiry.toExpiryDateOrNull(),
            accentColor = editingMedicine.color,
            containerColor = editingMedicine.backgroundColor,
            confirmLabel = "Save",
            onDismiss = { medicineBeingEdited = null },
            onConfirm = { name, expiryDate ->
                medicines = medicines.map { medicine ->
                    if (medicine.id == editingMedicine.id) {
                        medicine.copy(name = name, expiry = expiryDate.toExpiryLabel())
                    } else {
                        medicine
                    }
                }
                medicineBeingEdited = null
            },
        )
    }

    medicinePendingDelete?.let { deletingMedicine ->
        DeleteMedicineDialog(
            medicine = deletingMedicine,
            onDismiss = { medicinePendingDelete = null },
            onConfirm = {
                medicines = medicines.filterNot { it.id == deletingMedicine.id }
                if (selectedMedicineId == deletingMedicine.id) {
                    selectedMedicineId = null
                }
                medicinePendingDelete = null
            },
        )
    }
}

@Composable
private fun AppLoadingScreen() {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        color = Color(0xFFF7FAF8),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "Medicine Timer",
                modifier = Modifier.size(98.dp),
            )
        }
    }
}

private fun List<DoseReminder>.addReminder(time: String): List<DoseReminder> {
    if (any { it.time == time }) return this
    return (this + DoseReminder(time)).sortedBy { it.time.toReminderSortMinutes() }
}

private fun String.toReminderSortMinutes(): Int {
    return runCatching {
        val time = LocalTime.parse(this, reminderTimeFormatter)
        time.hour * 60 + time.minute
    }.getOrDefault(Int.MAX_VALUE)
}

private fun List<MedicineUi>.undoTakenDose(medicineId: Int, time: String): List<MedicineUi> {
    return map { medicine ->
        if (medicine.id == medicineId) {
            medicine.copy(
                reminders = medicine.reminders.map { reminder ->
                    if (reminder.time == time) reminder.copy(takenAt = null) else reminder
                },
            )
        } else {
            medicine
        }
    }
}

private const val medicinePrefsName = "medicine_timer"
private const val medicinesJsonKey = "medicines_json"

private fun loadMedicines(context: Context): List<MedicineUi> {
    val prefs = context.getSharedPreferences(medicinePrefsName, Context.MODE_PRIVATE)
    val json = prefs.getString(medicinesJsonKey, null) ?: return starterMedicines

    return runCatching {
        val medicinesJson = JSONArray(json)
        List(medicinesJson.length()) { index ->
            val medicineJson = medicinesJson.getJSONObject(index)
            val colorIndex = medicineJson.optInt(
                "colorIndex",
                positiveMod(medicineJson.getInt("id") - 1, medicineColors.size),
            ).let { positiveMod(it, medicineColors.size) }
            val remindersJson = medicineJson.optJSONArray("reminders") ?: JSONArray()

            MedicineUi(
                id = medicineJson.getInt("id"),
                name = medicineJson.getString("name"),
                expiry = medicineJson.getString("expiry"),
                color = medicineColors[colorIndex],
                backgroundColor = medicineBackgroundColors[colorIndex],
                surfaceColor = medicineSurfaceColors[colorIndex],
                reminders = List(remindersJson.length()) { reminderIndex ->
                    val reminderJson = remindersJson.getJSONObject(reminderIndex)
                    DoseReminder(
                        time = reminderJson.getString("time"),
                        takenAt = reminderJson.optString("takenAt")
                            .takeIf { it.isNotBlank() }
                            ?.let(LocalDateTime::parse),
                    )
                },
            )
        }
    }.getOrElse {
        starterMedicines
    }
}

private fun saveMedicines(context: Context, medicines: List<MedicineUi>) {
    val medicinesJson = JSONArray()
    medicines.forEach { medicine ->
        val remindersJson = JSONArray()
        medicine.reminders.forEach { reminder ->
            remindersJson.put(
                JSONObject()
                    .put("time", reminder.time)
                    .put("takenAt", reminder.takenAt?.toString().orEmpty()),
            )
        }

        medicinesJson.put(
            JSONObject()
                .put("id", medicine.id)
                .put("name", medicine.name)
                .put("expiry", medicine.expiry)
                .put("colorIndex", medicine.colorIndex())
                .put("reminders", remindersJson),
        )
    }

    context.getSharedPreferences(medicinePrefsName, Context.MODE_PRIVATE)
        .edit()
        .putString(medicinesJsonKey, medicinesJson.toString())
        .apply()
}

private fun MedicineUi.colorIndex(): Int {
    val colorIndex = medicineColors.indexOf(color)
    return if (colorIndex >= 0) colorIndex else positiveMod(id - 1, medicineColors.size)
}

private fun positiveMod(value: Int, divisor: Int): Int = ((value % divisor) + divisor) % divisor

private fun MedicineUi.isCompletedForDay(): Boolean {
    return reminders.isNotEmpty() && reminders.all { it.takenAt != null }
}

private fun LocalDate.toExpiryLabel(): String = "Expires ${format(expiryDateFormatter)}"

private fun String.toExpiryDateOrNull(): LocalDate? {
    val dateText = removePrefix("Expires").trim()
    return runCatching { LocalDate.parse(dateText, expiryDateFormatter) }.getOrNull()
}

private fun LocalDate.toDatePickerMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toDatePickerLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
}

@Composable
private fun CompletedLabel(
    completedAt: LocalDateTime,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    compact: Boolean = false,
    statusColor: Color = Color(0xFF2F6B5F),
    statusBackgroundColor: Color = Color(0xFFDDEFE9),
) {
    Column(horizontalAlignment = horizontalAlignment) {
        if (compact) {
            Text(
                text = "\u2713 Taken ${completedAt.format(completedTimeFormatter)}",
                color = statusColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
            )
        } else {
            Box(
                modifier = Modifier
                    .background(statusBackgroundColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "\u2713 Taken ${completedAt.format(completedTimeFormatter)}",
                    color = statusColor,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Text(
            text = completedAt.format(completedDateFormatter),
            color = Color(0xFF687A75),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun Header(
    title: String,
    page: Int,
    showBack: Boolean,
    onBack: () -> Unit,
    onPageChange: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16352F),
            )
            if (showBack) {
                TextButton(onClick = onBack) {
                    Text("Back")
                }
            }
        }
        if (!showBack) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NavigationTab(
                    label = "Medicines",
                    selected = page == 0,
                    onClick = { onPageChange(0) },
                )
                NavigationTab(
                    label = "Completed",
                    selected = page == 1,
                    onClick = { onPageChange(1) },
                )
                NavigationTab(
                    label = "Calendar",
                    selected = page == 2,
                    onClick = { onPageChange(2) },
                )
            }
        }
    }
}

@Composable
private fun NavigationTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF2F6B5F) else Color(0xFFE4E8E6),
            contentColor = if (selected) Color.White else Color(0xFF687A75),
        ),
    ) {
        Text(label)
    }
}

@Composable
private fun MedicinesPage(
    medicines: List<MedicineUi>,
    onMedicineClick: (MedicineUi) -> Unit,
    onAddMedicine: () -> Unit,
    onEditMedicine: (MedicineUi) -> Unit,
    onDeleteMedicine: (MedicineUi) -> Unit,
    onMarkTaken: (Int, String) -> Unit,
) {
    val activeMedicines = medicines.filterNot { it.isCompletedForDay() }
    val completedMedicines = medicines.filter { it.isCompletedForDay() }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = "Today",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF23443D),
            )
        }
        items(activeMedicines) { medicine ->
            MedicineCard(
                medicine = medicine,
                onClick = { onMedicineClick(medicine) },
                onEdit = { onEditMedicine(medicine) },
                onDelete = { onDeleteMedicine(medicine) },
                onMarkTaken = { time -> onMarkTaken(medicine.id, time) },
            )
        }
        if (completedMedicines.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HorizontalDivider(color = Color(0xFFD7E4E0))
                    Text(
                        text = "Completed for the day",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2F6B5F),
                    )
                }
            }
            items(completedMedicines) { medicine ->
                MedicineCard(
                    medicine = medicine,
                    onClick = { onMedicineClick(medicine) },
                    onEdit = { onEditMedicine(medicine) },
                    onDelete = { onDeleteMedicine(medicine) },
                    onMarkTaken = { time -> onMarkTaken(medicine.id, time) },
                )
            }
        }
        item {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onAddMedicine,
            ) {
                Text("Add medicine")
            }
        }
    }
}

@Composable
private fun MedicineCard(
    medicine: MedicineUi,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkTaken: (String) -> Unit,
) {
    val takenCount = medicine.reminders.count { it.takenAt != null }
    val completedForDay = medicine.isCompletedForDay()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = medicine.backgroundColor),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorDot(color = medicine.color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = medicine.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                MedicineCardStatus(
                    takenCount = takenCount,
                    completedForDay = completedForDay,
                    color = medicine.color,
                    backgroundColor = medicine.surfaceColor,
                )
            }
            MedicineReminderTasks(
                reminders = medicine.reminders,
                color = medicine.color,
                rowColor = medicine.surfaceColor,
                completedRowColor = medicine.surfaceColor,
                onMarkTaken = onMarkTaken,
            )
            Text(
                text = medicine.expiry,
                color = Color(0xFF687A75),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete", color = Color(0xFF9A3412))
                }
            }
        }
    }
}

@Composable
private fun MedicineCardStatus(
    takenCount: Int,
    completedForDay: Boolean,
    color: Color,
    backgroundColor: Color,
) {
    when {
        completedForDay -> Text(
            text = "Completed for the day",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )

        else -> Box(
            modifier = Modifier
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = "$takenCount taken",
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MedicineReminderTasks(
    reminders: List<DoseReminder>,
    color: Color,
    rowColor: Color,
    completedRowColor: Color,
    onMarkTaken: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        reminders.forEach { reminder ->
            val completedAt = reminder.takenAt
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (completedAt != null) completedRowColor else rowColor,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ColorDot(color = color)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = reminder.time,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF16352F),
                        )
                        Text(
                            text = if (completedAt != null) "Dose complete" else "Tap when taken",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF687A75),
                        )
                    }
                }
                if (completedAt != null) {
                    CompletedLabel(
                        completedAt = completedAt,
                        horizontalAlignment = Alignment.End,
                        compact = true,
                        statusColor = color,
                    )
                } else {
                    Button(
                        onClick = { onMarkTaken(reminder.time) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = color),
                    ) {
                        Text("Taken")
                    }
                }
            }
        }
    }
}

private data class CompletedReminder(
    val medicine: MedicineUi,
    val reminder: DoseReminder,
)

private data class CompletedMedicineGroup(
    val medicine: MedicineUi,
    val reminders: List<DoseReminder>,
)

@Composable
private fun CompletedPage(
    medicines: List<MedicineUi>,
    onUndoTaken: (Int, String) -> Unit,
) {
    val completedReminders = medicines.flatMap { medicine ->
        medicine.reminders
            .filter { it.takenAt != null }
            .map { reminder -> CompletedReminder(medicine, reminder) }
    }.sortedByDescending { it.reminder.takenAt }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                text = "Completed",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF23443D),
            )
        }
        if (completedReminders.isNotEmpty()) {
            item {
                CompletedTodaySection(
                    completedReminders = completedReminders,
                    onUndoTaken = onUndoTaken,
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Text(
                        modifier = Modifier.padding(16.dp),
                        text = "No completed doses yet.",
                        color = Color(0xFF687A75),
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedTodaySection(
    completedReminders: List<CompletedReminder>,
    onUndoTaken: (Int, String) -> Unit,
) {
    val completedGroups = completedReminders
        .groupBy { it.medicine.id }
        .map { (_, completedItems) ->
            CompletedMedicineGroup(
                medicine = completedItems.first().medicine,
                reminders = completedItems
                    .map { it.reminder }
                    .sortedBy { it.time },
            )
        }
        .sortedByDescending { group ->
            group.reminders.mapNotNull { it.takenAt }.maxOrNull()
        }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Completed today",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF23443D),
            )
            completedGroups.forEach { completedGroup ->
                CompletedMedicineGroupCard(
                    completedGroup = completedGroup,
                    onUndoTaken = { time -> onUndoTaken(completedGroup.medicine.id, time) },
                )
            }
        }
    }
}

@Composable
private fun CompletedMedicineGroupCard(
    completedGroup: CompletedMedicineGroup,
    onUndoTaken: (String) -> Unit,
) {
    val medicine = completedGroup.medicine

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(medicine.backgroundColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorDot(color = medicine.color)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = medicine.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16352F),
                )
            }
            Box(
                modifier = Modifier
                    .background(medicine.surfaceColor, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "${completedGroup.reminders.size} taken",
                    style = MaterialTheme.typography.labelMedium,
                    color = medicine.color,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        completedGroup.reminders.forEach { reminder ->
            CompletedDoseTaskRow(
                medicine = medicine,
                reminder = reminder,
                onUndoTaken = { onUndoTaken(reminder.time) },
            )
        }
    }
}

@Composable
private fun CompletedDoseTaskRow(
    medicine: MedicineUi,
    reminder: DoseReminder,
    onUndoTaken: () -> Unit,
) {
    val completedAt = reminder.takenAt ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(medicine.surfaceColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ColorDot(color = medicine.color)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = reminder.time,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF16352F),
                )
                Text(
                    text = "Dose complete",
                    color = Color(0xFF687A75),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            CompletedLabel(
                completedAt = completedAt,
                horizontalAlignment = Alignment.End,
                statusColor = medicine.color,
                statusBackgroundColor = medicine.backgroundColor,
                compact = true,
            )
            TextButton(onClick = onUndoTaken) {
                Text("Undo")
            }
        }
    }
}

@Composable
private fun MedicineDetailPage(
    medicine: MedicineUi,
    onEditMedicine: () -> Unit,
    onDeleteMedicine: () -> Unit,
    onAddReminder: () -> Unit,
    onMarkTaken: (String) -> Unit,
    onUndoTaken: (String) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = medicine.backgroundColor),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ColorDot(color = medicine.color)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = medicine.expiry,
                            color = Color(0xFF687A75),
                        )
                    }
                    Text(
                        text = "Set reminders",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Add reminder slots for the times this medicine should be taken today.",
                        color = Color(0xFF52645F),
                    )
                    Button(
                        onClick = onAddReminder,
                    ) {
                        Text("Add time")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onEditMedicine,
                        ) {
                            Text("Edit medicine")
                        }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onDeleteMedicine,
                        ) {
                            Text("Delete", color = Color(0xFF9A3412))
                        }
                    }
                }
            }
        }
        items(medicine.reminders) { reminder ->
            ReminderRow(
                reminder = reminder,
                color = medicine.color,
                backgroundColor = medicine.backgroundColor,
                surfaceColor = medicine.surfaceColor,
                onMarkTaken = { onMarkTaken(reminder.time) },
                onUndoTaken = { onUndoTaken(reminder.time) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    medicine: MedicineUi,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val defaultTime = remember {
        LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    }
    val timePickerState = rememberTimePickerState(
        initialHour = defaultTime.hour,
        initialMinute = defaultTime.minute,
        is24Hour = false,
    )
    val selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
        .format(reminderTimeFormatter)
    val alreadyExists = medicine.reminders.any { it.time == selectedTime }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = medicine.backgroundColor,
        title = { Text("Add reminder") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TimeInput(state = timePickerState)
                Text(
                    text = if (alreadyExists) "This time is already set." else "Selected: $selectedTime",
                    color = if (alreadyExists) Color(0xFF9A3412) else Color(0xFF52645F),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedTime) },
                enabled = !alreadyExists,
                colors = ButtonDefaults.buttonColors(containerColor = medicine.color),
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedicineEditorDialog(
    title: String,
    initialName: String,
    initialExpiryDate: LocalDate?,
    accentColor: Color,
    containerColor: Color,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate) -> Unit,
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var expiryDate by remember(initialExpiryDate) {
        mutableStateOf(initialExpiryDate ?: LocalDate.now().plusMonths(6))
    }
    var showExpiryPicker by remember { mutableStateOf(false) }
    val canSave = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Medicine name") },
                    singleLine = true,
                )
                OutlinedButton(
                    onClick = { showExpiryPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Expiry date",
                            color = Color(0xFF60716B),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = expiryDate.format(expiryDateFormatter),
                            color = Color(0xFF143D35),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), expiryDate) },
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )

    if (showExpiryPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiryDate.toDatePickerMillis(),
        )

        DatePickerDialog(
            onDismissRequest = { showExpiryPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            expiryDate = selectedMillis.toDatePickerLocalDate()
                        }
                        showExpiryPicker = false
                    },
                ) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExpiryPicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DeleteMedicineDialog(
    medicine: MedicineUi,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Are you sure?") },
        text = {
            Text("Delete ${medicine.name}? This will delete its reminders and history from this device.")
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes, delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ReminderRow(
    reminder: DoseReminder,
    color: Color,
    backgroundColor: Color,
    surfaceColor: Color,
    onMarkTaken: () -> Unit,
    onUndoTaken: () -> Unit,
) {
    val completedAt = reminder.takenAt

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(color = color)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(reminder.time, fontWeight = FontWeight.SemiBold)
                    if (completedAt != null) {
                        CompletedLabel(
                            completedAt = completedAt,
                            statusColor = color,
                            statusBackgroundColor = backgroundColor,
                        )
                    } else {
                        Text(
                            text = "Left today",
                            color = Color(0xFF8A5E00),
                        )
                    }
                }
            }
            if (completedAt != null) {
                OutlinedButton(onClick = onUndoTaken) {
                    Text("Undo")
                }
            } else {
                OutlinedButton(onClick = onMarkTaken) {
                    Text("Taken")
                }
            }
        }
    }
}

@Composable
private fun ReminderProgress(reminders: List<DoseReminder>, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        reminders.forEach { reminder ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val completedAt = reminder.takenAt
                Text(text = reminder.time)
                    if (completedAt != null) {
                        CompletedLabel(completedAt = completedAt)
                    } else {
                        Text(
                            text = "Not taken",
                            color = Color(0xFF8A5E00),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
            }
        }
    }
}

@Composable
private fun CalendarPage(
    medicines: List<MedicineUi>,
) {
    var selectedDay by remember { mutableIntStateOf(12) }
    val completedForSelectedDay = medicines.flatMap { medicine ->
        medicine.reminders
            .filter { reminder -> reminder.takenAt?.dayOfMonth == selectedDay }
            .map { reminder -> CompletedReminder(medicine, reminder) }
    }.sortedBy { it.reminder.takenAt }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(
                text = "Calendar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF23443D),
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("July 2026", fontWeight = FontWeight.SemiBold)
                        Text("Select dates", color = Color(0xFF687A75))
                    }
                    CalendarGrid(
                        selectedDay = selectedDay,
                        medicines = medicines,
                        onDaySelected = { selectedDay = it },
                    )
                    Text(
                        text = "Selected: $selectedDay Jul 2026",
                        color = Color(0xFF687A75),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (completedForSelectedDay.isNotEmpty()) {
                        completedForSelectedDay.forEach { completed ->
                            CalendarTakenRow(
                                medicine = completed.medicine,
                                reminder = completed.reminder,
                            )
                        }
                    } else {
                        Text("No medicines taken on this date.", color = Color(0xFF687A75))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    selectedDay: Int,
    medicines: List<MedicineUi>,
    onDaySelected: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE9F2EF), RoundedCornerShape(8.dp))
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
        }
        (0 until 5).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                (1..7).forEach { dayOfWeek ->
                    val day = week * 7 + dayOfWeek
                    CalendarDayCell(
                        day = day,
                        selected = day == selectedDay,
                        markerColors = markerColorsForDay(day, medicines),
                        markerBackgroundColor = markerBackgroundForDay(day, medicines),
                        onClick = { onDaySelected(day) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    selected: Boolean,
    markerColors: List<Color>,
    markerBackgroundColor: Color?,
    onClick: () -> Unit,
) {
    val backgroundColor = when {
        selected -> Color(0xFFE0EFEA)
        markerBackgroundColor != null -> markerBackgroundColor
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier
            .width(38.dp)
            .height(54.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(day.toString(), style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            markerColors.take(4).forEach { color ->
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(color, CircleShape),
                )
            }
        }
    }
}

private fun markerColorsForDay(day: Int, medicines: List<MedicineUi>): List<Color> {
    return medicines.mapNotNull { medicine ->
        if (medicine.reminders.any { reminder -> reminder.takenAt?.dayOfMonth == day }) {
            medicine.color
        } else {
            null
        }
    }
}

private fun markerBackgroundForDay(day: Int, medicines: List<MedicineUi>): Color? {
    val matchingMedicines = medicines.filter { medicine ->
        medicine.reminders.any { reminder -> reminder.takenAt?.dayOfMonth == day }
    }
    return when (matchingMedicines.size) {
        0 -> null
        1 -> matchingMedicines.first().backgroundColor
        else -> Color(0xFFF2F7F5)
    }
}

@Composable
private fun CalendarTakenRow(
    medicine: MedicineUi,
    reminder: DoseReminder,
) {
    val completedAt = reminder.takenAt ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(medicine.backgroundColor, RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ColorDot(color = medicine.color)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(medicine.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${reminder.time} dose",
                        color = Color(0xFF687A75),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            CompletedLabel(
                completedAt = completedAt,
                horizontalAlignment = Alignment.End,
                statusColor = medicine.color,
                statusBackgroundColor = medicine.surfaceColor,
            )
        }
    }
}

@Composable
private fun ColorDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape),
    )
}

@Composable
private fun MedicineTimerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2F6B5F),
            secondary = Color(0xFF7A5C2E),
            background = Color(0xFFF7FAF8),
            surface = Color.White,
        ),
        content = content,
    )
}

@Preview(showBackground = true)
@Composable
private fun MedicineTimerPreview() {
    MedicineTimerTheme {
        MedicineTimerApp()
    }
}
