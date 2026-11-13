package com.egepaksoy.interner.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    // Çift dokunuşta ana ekranın da yığından atılmasını önler.
    val back: () -> Unit = { if (nav.previousBackStackEntry != null) nav.popBackStack() }
    val idArg = listOf(navArgument("id") { type = NavType.LongType })

    NavHost(nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = vm,
                onEntry = { nav.navigate("entry/$it") },
                onNewEntry = { date -> nav.navigate("entry/0?date=$date") },
                onNote = { nav.navigate("note/$it") },
                onInternship = { nav.navigate("internship") },
            )
        }
        composable(
            "entry/{id}?date={date}",
            arguments = idArg + navArgument("date") { type = NavType.LongType; defaultValue = NO_DATE },
        ) {
            val date = it.arguments?.getLong("date")?.takeIf { d -> d != NO_DATE }
            EntryEditScreen(vm, it.arguments?.getLong("id") ?: 0, date, back)
        }
        composable("note/{id}", arguments = idArg) {
            NoteEditScreen(vm, it.arguments?.getLong("id") ?: 0, back)
        }
        composable("internship") {
            InternshipScreen(vm, back)
        }
    }
}

/** Yeni kayıt için tarih verilmediğini (bugün kullanılacağını) belirtir. */
private const val NO_DATE = Long.MIN_VALUE

private val tabs = listOf(
    Triple("Günlük", "Staj Günlüğü", Icons.Default.DateRange),
    Triple("Notlar", "Notlar", Icons.Default.Create),
    Triple("Özet", "Özet", Icons.Default.Info),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    vm: AppViewModel,
    onEntry: (Long) -> Unit,
    onNewEntry: (Long) -> Unit,
    onNote: (Long) -> Unit,
    onInternship: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val entries by vm.entries.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val internship by vm.internship.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text(tabs[tab].second) }) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, (label, _, icon) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(icon, contentDescription = null) },
                        label = { Text(label) },
                    )
                }
            }
        },
        floatingActionButton = {
            when (tab) {
                0 -> ExtendedFloatingActionButton(
                    onClick = { onEntry(0) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Gün ekle") },
                )
                1 -> ExtendedFloatingActionButton(
                    onClick = { onNote(0) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Not ekle") },
                )
            }
        },
    ) { pad ->
        val e = entries
        val n = notes
        when (tab) {
            0 -> if (e != null) LogScreen(e, internship, pad, onEntry, onNewEntry, onInternship)
            1 -> if (n != null) NotesScreen(n, pad, onNote, vm::setPinned)
            else -> if (e != null) SummaryScreen(vm, e, internship, pad, onInternship)
        }
    }
}
