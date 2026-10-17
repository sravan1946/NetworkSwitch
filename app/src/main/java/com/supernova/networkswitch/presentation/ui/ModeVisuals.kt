package com.supernova.networkswitch.presentation.ui

import androidx.annotation.DrawableRes
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import com.supernova.networkswitch.R
import com.supernova.networkswitch.domain.model.NetworkMode

/** Monochrome glyph for the tile and badges, by the highest generation a mode allows. */
@DrawableRes
fun NetworkMode?.iconRes(): Int = when (this?.highestGeneration) {
    5 -> R.drawable.ic_mode_5g
    4 -> R.drawable.ic_mode_4g
    3 -> R.drawable.ic_mode_3g
    2 -> R.drawable.ic_mode_2g
    else -> R.drawable.ic_mode_auto
}

/** Each kind of mode gets its own silhouette so a switch reads as a shape change. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun NetworkMode?.polygon(): RoundedPolygon {
    if (this == null) return MaterialShapes.Circle
    val single = generations.size == 1
    return when (highestGeneration) {
        5 -> if (single) MaterialShapes.Cookie9Sided else MaterialShapes.SoftBurst
        4 -> if (single) MaterialShapes.Cookie4Sided else MaterialShapes.Clover8Leaf
        3 -> if (single) MaterialShapes.Pentagon else MaterialShapes.Puffy
        else -> MaterialShapes.Arch
    }
}

/**
 * Clips to [morph] at [progress], turned by [rotation] degrees about its centre.
 * Polygons are in the unit square, so the path is scaled up to the layout size.
 */
class MorphShape(
    private val morph: Morph,
    private val progress: Float,
    private val rotation: Float = 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        morph.asCubics(progress).forEachIndexed { i, c ->
            if (i == 0) path.moveTo(c.anchor0X, c.anchor0Y)
            path.cubicTo(c.control0X, c.control0Y, c.control1X, c.control1Y, c.anchor1X, c.anchor1Y)
        }
        path.close()
        path.transform(Matrix().apply {
            scale(size.width, size.height)
            translate(0.5f, 0.5f)
            rotateZ(rotation)
            translate(-0.5f, -0.5f)
        })
        return Outline.Generic(path)
    }
}
