package neumann.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import neumann.core.data.GameDataLoader
import neumann.core.game.GameSession
import kotlin.random.Random

/** Hosts the approach screen for a random system, then the bridge. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = GameDataLoader(AssetDataSource(assets)).load()
        val version = BuildConfig.VERSION_NAME
        setContent {
            var seed by remember { mutableStateOf(newSeed()) }
            var session by remember { mutableStateOf<GameSession?>(null) }
            Box(Modifier.fillMaxSize().background(Theme.background).safeDrawingPadding()) {
                val s = session
                if (s == null) {
                    ApproachScreen(data, seed, version, onNewSystem = { seed = newSeed() }) { scenario, approach ->
                        session = GameSession(data, scenario, approach)
                    }
                } else {
                    BridgeScreen(s, version) { seed = newSeed(); session = null }
                }
            }
        }
    }

    /** Six-digit seeds, short enough to read off the screen and replay. */
    private fun newSeed() = Random.nextLong(100_000, 1_000_000)
}
