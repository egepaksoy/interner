package com.egepaksoy.interner

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.egepaksoy.interner.ui.AppNav
import com.egepaksoy.interner.ui.theme.InternerTheme

class MainActivity : ComponentActivity() {
    /** Widget'tan açılması istenen ekran; açıldıktan sonra temizlenir. */
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Ekran döndürme gibi yeniden oluşturmalarda aynı niyet ikinci kez işlenmesin.
        if (savedInstanceState == null) pendingRoute = routeOf(intent)
        setContent {
            InternerTheme {
                AppNav(pendingRoute, onRouteConsumed = { pendingRoute = null })
            }
        }
    }

    public override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeOf(intent)?.let { pendingRoute = it }
    }

    /** Etkinlik dışa açık olduğu için yalnızca bilinen biçimdeki rotalar kabul edilir. */
    private fun routeOf(intent: Intent?): String? =
        intent?.getStringExtra(EXTRA_ROUTE)?.takeIf { ROUTE.matches(it) }

    companion object {
        const val EXTRA_ROUTE = "route"
        private val ROUTE = Regex("""entry/\d+(\?date=-?\d+)?""")
    }
}
