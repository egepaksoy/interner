package com.egepaksoy.interner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.egepaksoy.interner.data.Internship
import java.time.DayOfWeek
import java.time.LocalDate

/** İki tarih arasındaki (uçlar dahil) hafta içi gün sayısı. */
fun weekdaysBetween(start: Long, end: Long): Int =
    (start..end).count { LocalDate.ofEpochDay(it).dayOfWeek < DayOfWeek.SATURDAY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternshipScreen(vm: AppViewModel, onBack: () -> Unit) {
    val saved by vm.internship.collectAsStateWithLifecycle()
    var ready by rememberSaveable { mutableStateOf(false) }
    var company by rememberSaveable { mutableStateOf("") }
    var department by rememberSaveable { mutableStateOf("") }
    var start by rememberSaveable { mutableStateOf<Long?>(null) }
    var end by rememberSaveable { mutableStateOf<Long?>(null) }
    var target by rememberSaveable { mutableStateOf("") }
    var targetEdited by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(saved) {
        val s = saved
        if (!ready && s != null) {
            company = s.company
            department = s.department
            start = s.startDate
            end = s.endDate
            target = if (s.targetDays > 0) s.targetDays.toString() else ""
            targetEdited = s.targetDays > 0
            ready = true
        }
    }

    // Hedef elle girilmediyse tarihlerden öner.
    fun suggestTarget() {
        val s = start
        val e = end
        if (!targetEdited && s != null && e != null && e >= s) target = weekdaysBetween(s, e).toString()
    }

    val datesInvalid = start != null && end != null && end!! < start!!

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Staj bilgileri") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            vm.save(
                                Internship(
                                    company = company.trim(),
                                    department = department.trim(),
                                    startDate = start,
                                    endDate = end,
                                    targetDays = target.toIntOrNull()
                                        ?: start?.let { s -> end?.let { e -> weekdaysBetween(s, e) } }
                                        ?: 0,
                                ),
                                onBack,
                            )
                        },
                        enabled = !datesInvalid,
                    ) { Text("Kaydet") }
                },
            )
        },
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .consumeWindowInsets(pad)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            OutlinedTextField(
                value = company,
                onValueChange = { company = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kurum / bakım kuruluşu") },
                singleLine = true,
                keyboardOptions = words,
            )
            OutlinedTextField(
                value = department,
                onValueChange = { department = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bölüm / konum") },
                placeholder = { Text("Örn. Hangar 2, hat bakım") },
                singleLine = true,
                keyboardOptions = words,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DateRow("Başlangıç", start, { start = it; suggestTarget() }, Modifier.weight(1f))
                DateRow("Bitiş", end, { end = it; suggestTarget() }, Modifier.weight(1f))
            }
            OutlinedTextField(
                value = target,
                onValueChange = { v -> target = v.filter(Char::isDigit).take(3); targetEdited = target.isNotEmpty() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Hedef staj günü sayısı") },
                supportingText = {
                    Text(
                        if (datesInvalid) "Bitiş tarihi başlangıçtan önce olamaz"
                        else "Boş bırakırsan tarihler arasındaki hafta içi günlerden hesaplanır",
                    )
                },
                isError = datesInvalid,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }
    }
}
