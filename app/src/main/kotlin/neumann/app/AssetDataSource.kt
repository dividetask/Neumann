package neumann.app

import android.content.res.AssetManager
import neumann.core.data.DataSource

/** Reads data files from the APK's assets (the repo's data/ folder). */
class AssetDataSource(private val assets: AssetManager) : DataSource {
    override fun read(fileName: String): String = assets.open(fileName).bufferedReader().use { it.readText() }
}
