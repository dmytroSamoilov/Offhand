package com.dmytrosamoilov.offhand.core.designsystem.component

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.star
import androidx.graphics.shapes.toPath

val CookiePolygon: RoundedPolygon = RoundedPolygon.star(
    numVerticesPerRadius = 9,
    innerRadius = 0.82f,
    rounding = CornerRounding(radius = 0.24f),
).normalized()

val ScallopPolygon: RoundedPolygon = RoundedPolygon.star(
    numVerticesPerRadius = 12,
    innerRadius = 0.9f,
    rounding = CornerRounding(radius = 0.16f),
).normalized()

val CookieShape: Shape = RoundedPolygonShape(CookiePolygon)

class RoundedPolygonShape(private val polygon: RoundedPolygon) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path = polygon.toPath().asComposePath()
        val matrix = Matrix()
        matrix.scale(x = size.width, y = size.height)
        path.transform(matrix)
        return Outline.Generic(path)
    }
}
