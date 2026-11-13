package com.egepaksoy.interner.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.egepaksoy.interner.InternerApp
import com.egepaksoy.interner.R
import com.egepaksoy.interner.data.fullDate

/** Yeni kayıt için tarih verilmediğini (bugün kullanılacağını) belirtir. */
private const val NO_DATE = Long.MIN_VALUE

/** Bir günlük kaydını (0 = yeni kayıt) açan rota; widget'lar da bunu kullanır. */
fun entryRoute(id: Long, date: Long? = null): String =
    if (date != null) "entry/$id?date=$date" else "entry/$id"

/**
 * @param pendingRoute uygulama dışından (widget) açılması istenen ekran; açılınca
 * [onRouteConsumed] çağrılır.
 */
@Composable
fun AppNav(pendingRoute: String? = null, onRouteConsumed: () -> Unit = {}) {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as InternerApp
    val vm: AppViewModel = viewModel(factory = viewModelFactory { initializer { AppViewModel(app.db.dao()) } })
    // Çift dokunuşta ana ekranın da yığından atılmasını önler.
    val back: () -> Unit = { if (nav.previousBackStackEntry != null) nav.popBackStack() }
    val idArg = listOf(navArgument("id") { type = NavType.LongType })

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            nav.popBackStack("home", inclusive = false)
            nav.navigate(pendingRoute)
            onRouteConsumed()
        }
    }

    // Ekranlar arası geçişler bilerek animasyonsuz.
    NavHost(
        navController = nav,
        startDestination = "home",
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None },
        sizeTransform = null,
    ) {
        composable("home") {
            HomeScreen(
                vm = vm,
                onEntry = { nav.navigate(entryRoute(it)) },
                onNewEntry = { date -> nav.navigate(entryRoute(0, date)) },
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

private class Tab(val label: String, val title: String, val icon: Int)

private val tabs = listOf(
    Tab("Günlük", "Staj Günlüğü", R.drawable.ic_calendar),
    Tab("Notlar", "Notlar", R.drawable.ic_note),
    Tab("Özet", "Özet", R.drawable.ic_chart),
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
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_plane),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(tabs[tab].title, style = MaterialTheme.typography.titleLarge)
                            Text(
                                fullDate(today()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                    tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(painterResource(t.icon), contentDescription = null) },
                            label = { Text(t.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val (label, action) = when (tab) {
                0 -> "Gün ekle" to { onEntry(0) }
                1 -> "Not ekle" to { onNote(0) }
                else -> return@Scaffold
            }
            ExtendedFloatingActionButton(
                onClick = action,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(label) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
            )
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
