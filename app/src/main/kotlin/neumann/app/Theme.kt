package neumann.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

/** Ship-display colours and text styles. */
object Theme {
    val background = Color(0xFF050A05)
    val panel = Color(0xFF0B140B)
    val text = Color(0xFF7CFC7C)
    val dim = Color(0xFF3F7F3F)
    val alert = Color(0xFFFFB000)
    val danger = Color(0xFFFF4D4D)
    val star = Color(0xFFFFE070)
    val planet = Color(0xFF6FA8DC)
    val player = Color(0xFF7CFC7C)
    val enemy = Color(0xFFFF4D4D)

    val body = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = text)
    val small = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = text)
    val heading = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp, color = text)
}
