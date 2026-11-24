package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object OffhandIcons {
    val Search = stroked("search", "M4 11a7 7 0 1 0 14 0a7 7 0 1 0 -14 0", "M20 20l-3.5-3.5")
    val Plus = stroked("plus", "M12 5v14M5 12h14")
    val Back = stroked("back", "M15 5l-7 7 7 7", autoMirror = true)
    val ChevronRight = stroked("chevron_right", "M9 5l7 7-7 7", width = 2f, autoMirror = true)
    val ChevronDown = stroked("chevron_down", "M6 9l6 6 6-6", width = 2f)
    val Close = stroked("close", "M6 6l12 12M18 6L6 18")
    val Check = stroked("check", "M5 12.5l4.5 4.5L19 7.5", width = 3.5f)
    val Share = stroked("share", "M12 15V4M8 8l4-4 4 4M5 13v5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-5")
    val Download = stroked("download", "M12 4v11M8 11l4 4 4-4M5 19h14")
    val Copy = stroked(
        "copy",
        "M10.5 8h7a2.5 2.5 0 0 1 2.5 2.5v7a2.5 2.5 0 0 1-2.5 2.5h-7a2.5 2.5 0 0 1-2.5-2.5v-7a2.5 2.5 0 0 1 2.5-2.5z",
        "M16 8V6.5A2.5 2.5 0 0 0 13.5 4h-7A2.5 2.5 0 0 0 4 6.5v7A2.5 2.5 0 0 0 6.5 16H8",
        width = 1.9f,
    )
    val Notes = stroked("notes", "M5 7h14M5 12h14M5 17h9")
    val Settings = stroked(
        "settings",
        "M4 7h10M18 7h2M4 17h2M10 17h10",
        "M14 7a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
        "M6 17a2 2 0 1 0 4 0a2 2 0 1 0 -4 0",
    )
    val Lock = stroked("lock", "M7 11h10a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2z", "M8 11V8a4 4 0 0 1 8 0v3")
    val Unlock = stroked(
        "unlock",
        "M7.5 10.5h9a2.5 2.5 0 0 1 2.5 2.5v4.5a2.5 2.5 0 0 1-2.5 2.5h-9a2.5 2.5 0 0 1-2.5-2.5V13a2.5 2.5 0 0 1 2.5-2.5z",
        "M8.5 10.5V8a3.5 3.5 0 0 1 6.6-1.6",
    )
    val Folder = stroked("folder", "M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
    val More = filled(
        "more",
        "M3.4 12a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0" +
            "M10.4 12a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0" +
            "M17.4 12a1.6 1.6 0 1 0 3.2 0a1.6 1.6 0 1 0 -3.2 0",
    )
    val Play = filled("play", "M8 5.5v13a1 1 0 0 0 1.5.86l11-6.5a1 1 0 0 0 0-1.72l-11-6.5A1 1 0 0 0 8 5.5z")
    val Pause = filled("pause", "M7 5h2a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1zM15 5h2a1 1 0 0 1 1 1v12a1 1 0 0 1-1 1h-2a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z")
    val PauseBars = stroked("pause_bars", "M9 6v12M15 6v12", width = 2.5f)
    val Stop = filled("stop", "M8 6h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z")
    val Edit = stroked("edit", "M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16z", "M13.5 6.5l4 4")
    val Trash = stroked("trash", "M4 7h16M10 11v6M14 11v6M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2")
    val Info = stroked("info", "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0", "M12 11v5M12 8h.01", width = 1.75f)
    val Mic = stroked("mic", "M9 6a3 3 0 0 1 6 0v6a3 3 0 0 1-6 0z", "M5 11a7 7 0 0 0 14 0M12 18v3")
    val Calendar = stroked("calendar", "M6 5h12a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7a2 2 0 0 1 2-2z", "M4 10h16M8 3v4M16 3v4")
    val Sparkle = stroked("sparkle", "M12 4l1.8 4.9L19 10.5l-5.2 1.6L12 17l-1.8-4.9L5 10.5l5.2-1.6z")

    private fun stroked(name: String, vararg paths: String, width: Float = 1.75f, autoMirror: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = autoMirror).apply {
            paths.forEach { data ->
                addPath(
                    pathData = PathParser().parsePathString(data).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = width,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    private fun filled(name: String, data: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(pathData = PathParser().parsePathString(data).toNodes(), fill = SolidColor(Color.Black))
        }.build()
}
