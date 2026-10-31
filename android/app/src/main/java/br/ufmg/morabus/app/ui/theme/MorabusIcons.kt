package br.ufmg.morabus.app.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Ícones vetoriais leves do Morabus UFMG.
 * Substitui a dependência pesada material-icons-extended (que gerava um classes.dex de 43 MB).
 */
object MorabusIcons {

    val DirectionsBus: ImageVector by lazy {
        ImageVector.Builder(
            name = "DirectionsBus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(4f, 16f)
                curveTo(4f, 16.88f, 4.39f, 17.67f, 5f, 18.22f)
                verticalLineTo(20f)
                curveTo(5f, 20.55f, 5.45f, 21f, 6f, 21f)
                horizontalLineTo(7f)
                curveTo(7.55f, 21f, 8f, 20.55f, 8f, 20f)
                verticalLineTo(19f)
                horizontalLineTo(16f)
                verticalLineTo(20f)
                curveTo(16f, 20.55f, 16.45f, 21f, 17f, 21f)
                horizontalLineTo(18f)
                curveTo(18.55f, 21f, 19f, 20.55f, 19f, 20f)
                verticalLineTo(18.22f)
                curveTo(19.61f, 17.67f, 20f, 16.88f, 20f, 16f)
                verticalLineTo(6f)
                curveTo(20f, 2.5f, 16.42f, 2f, 12f, 2f)
                curveTo(7.58f, 2f, 4f, 2.5f, 4f, 6f)
                verticalLineTo(16f)
                close()
                moveTo(7.5f, 17f)
                curveTo(6.67f, 17f, 6f, 16.33f, 6f, 15.5f)
                curveTo(6f, 14.67f, 6.67f, 14f, 7.5f, 14f)
                curveTo(8.33f, 14f, 9f, 14.67f, 9f, 15.5f)
                curveTo(9f, 16.33f, 8.33f, 17f, 7.5f, 17f)
                close()
                moveTo(16.5f, 17f)
                curveTo(15.67f, 17f, 15f, 16.33f, 15f, 15.5f)
                curveTo(15f, 14.67f, 15.67f, 14f, 16.5f, 14f)
                curveTo(17.33f, 14f, 18f, 14.67f, 18f, 15.5f)
                curveTo(18f, 16.33f, 17.33f, 17f, 16.5f, 17f)
                close()
                moveTo(18f, 11f)
                horizontalLineTo(6f)
                verticalLineTo(6f)
                horizontalLineTo(18f)
                verticalLineTo(11f)
                close()
            }
        }.build()
    }

    val Map: ImageVector by lazy {
        ImageVector.Builder(
            name = "Map",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20.5f, 3f)
                lineTo(20.34f, 3.03f)
                lineTo(15f, 5.1f)
                lineTo(9f, 3f)
                lineTo(3.36f, 4.9f)
                curveTo(3.15f, 4.97f, 3f, 5.15f, 3f, 5.38f)
                verticalLineTo(20.5f)
                curveTo(3f, 20.78f, 3.22f, 21f, 3.5f, 21f)
                lineTo(3.66f, 20.97f)
                lineTo(9f, 18.9f)
                lineTo(15f, 21f)
                lineTo(20.64f, 19.1f)
                curveTo(20.85f, 19.03f, 21f, 18.85f, 21f, 18.62f)
                verticalLineTo(3.5f)
                curveTo(21f, 3.22f, 20.78f, 3f, 20.5f, 3f)
                close()
                moveTo(15f, 19f)
                lineTo(9f, 16.89f)
                verticalLineTo(5f)
                lineTo(15f, 7.11f)
                verticalLineTo(19f)
                close()
            }
        }.build()
    }

    val Schedule: ImageVector by lazy {
        ImageVector.Builder(
            name = "Schedule",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11.99f, 2f)
                curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveTo(6.47f, 22f, 11.99f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 11.99f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
                reflectiveCurveTo(7.58f, 4f, 12f, 4f)
                reflectiveCurveTo(20f, 7.58f, 20f, 12f)
                reflectiveCurveTo(16.42f, 20f, 12f, 20f)
                close()
                moveTo(12.5f, 7f)
                horizontalLineTo(11f)
                verticalLineTo(13f)
                lineTo(16.25f, 16.15f)
                lineTo(17f, 14.92f)
                lineTo(12.5f, 12.25f)
                verticalLineTo(7f)
                close()
            }
        }.build()
    }

    val MoreHoriz: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreHoriz",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 10f)
                curveTo(4.9f, 10f, 4f, 10.9f, 4f, 12f)
                reflectiveCurveTo(4.9f, 14f, 6f, 14f)
                reflectiveCurveTo(8f, 13.1f, 8f, 12f)
                reflectiveCurveTo(7.1f, 10f, 6f, 10f)
                close()
                moveTo(18f, 10f)
                curveTo(16.9f, 10f, 16f, 10.9f, 16f, 12f)
                reflectiveCurveTo(16.9f, 14f, 18f, 14f)
                reflectiveCurveTo(20f, 13.1f, 20f, 12f)
                reflectiveCurveTo(19.1f, 10f, 18f, 10f)
                close()
                moveTo(12f, 10f)
                curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
                reflectiveCurveTo(10.9f, 14f, 12f, 14f)
                reflectiveCurveTo(14f, 13.1f, 14f, 12f)
                reflectiveCurveTo(13.1f, 10f, 12f, 10f)
                close()
            }
        }.build()
    }

    val MyLocation: ImageVector by lazy {
        ImageVector.Builder(
            name = "MyLocation",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 8f)
                curveTo(9.79f, 8f, 8f, 9.79f, 8f, 12f)
                reflectiveCurveTo(9.79f, 16f, 12f, 16f)
                reflectiveCurveTo(16f, 14.21f, 16f, 12f)
                reflectiveCurveTo(14.21f, 8f, 12f, 8f)
                close()
                moveTo(20.94f, 11f)
                curveTo(20.48f, 6.83f, 17.17f, 3.52f, 13f, 3.06f)
                verticalLineTo(1f)
                horizontalLineTo(11f)
                verticalLineTo(3.06f)
                curveTo(6.83f, 3.52f, 3.52f, 6.83f, 3.06f, 11f)
                horizontalLineTo(1f)
                verticalLineTo(13f)
                horizontalLineTo(3.06f)
                curveTo(3.52f, 17.17f, 6.83f, 20.48f, 11f, 20.94f)
                verticalLineTo(23f)
                horizontalLineTo(13f)
                verticalLineTo(20.94f)
                curveTo(17.17f, 20.48f, 20.48f, 17.17f, 20.94f, 13f)
                horizontalLineTo(23f)
                verticalLineTo(11f)
                horizontalLineTo(20.94f)
                close()
                moveTo(12f, 19f)
                curveTo(8.13f, 19f, 5f, 15.87f, 5f, 12f)
                reflectiveCurveTo(8.13f, 5f, 12f, 5f)
                reflectiveCurveTo(19f, 8.13f, 19f, 12f)
                reflectiveCurveTo(15.87f, 19f, 12f, 19f)
                close()
            }
        }.build()
    }

    val Traffic: ImageVector by lazy {
        ImageVector.Builder(
            name = "Traffic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(20f, 10f)
                horizontalLineTo(17f)
                verticalLineTo(8.86f)
                curveTo(18.72f, 8.41f, 20f, 6.86f, 20f, 5f)
                horizontalLineTo(17f)
                verticalLineTo(4f)
                curveTo(17f, 3.45f, 16.55f, 3f, 16f, 3f)
                horizontalLineTo(8f)
                curveTo(7.45f, 3f, 7f, 3.45f, 7f, 4f)
                verticalLineTo(5f)
                horizontalLineTo(4f)
                curveTo(4f, 6.86f, 5.28f, 8.41f, 7f, 8.86f)
                verticalLineTo(10f)
                horizontalLineTo(4f)
                curveTo(4f, 11.86f, 5.28f, 13.41f, 7f, 13.86f)
                verticalLineTo(15f)
                horizontalLineTo(4f)
                curveTo(4f, 16.86f, 5.28f, 18.41f, 7f, 18.86f)
                verticalLineTo(20f)
                curveTo(7f, 20.55f, 7.45f, 21f, 8f, 21f)
                horizontalLineTo(16f)
                curveTo(16.55f, 21f, 17f, 20.55f, 17f, 20f)
                verticalLineTo(18.86f)
                curveTo(18.72f, 18.41f, 20f, 16.86f, 20f, 15f)
                horizontalLineTo(17f)
                verticalLineTo(13.86f)
                curveTo(18.72f, 13.41f, 20f, 11.86f, 20f, 10f)
                close()
                moveTo(12f, 19f)
                curveTo(10.9f, 19f, 10f, 18.1f, 10f, 17f)
                reflectiveCurveTo(10.9f, 15f, 12f, 15f)
                reflectiveCurveTo(14f, 15.9f, 14f, 17f)
                reflectiveCurveTo(13.1f, 19f, 12f, 19f)
                close()
                moveTo(12f, 14f)
                curveTo(10.9f, 14f, 10f, 13.1f, 10f, 12f)
                reflectiveCurveTo(10.9f, 10f, 12f, 10f)
                reflectiveCurveTo(14f, 10.9f, 14f, 12f)
                reflectiveCurveTo(13.1f, 14f, 12f, 14f)
                close()
                moveTo(12f, 9f)
                curveTo(10.9f, 9f, 10f, 8.1f, 10f, 7f)
                reflectiveCurveTo(10.9f, 5f, 12f, 5f)
                reflectiveCurveTo(14f, 5.9f, 14f, 7f)
                reflectiveCurveTo(13.1f, 9f, 12f, 9f)
                close()
            }
        }.build()
    }

    val WaterDrop: ImageVector by lazy {
        ImageVector.Builder(
            name = "WaterDrop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.67f, 6.55f, 4f, 10.48f, 4f, 13.8f)
                curveTo(4f, 18.78f, 7.8f, 22f, 12f, 22f)
                reflectiveCurveTo(20f, 18.78f, 20f, 13.8f)
                curveTo(20f, 10.48f, 17.33f, 6.55f, 12f, 2f)
                close()
            }
        }.build()
    }

    val Groups: ImageVector by lazy {
        ImageVector.Builder(
            name = "Groups",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 11f)
                curveTo(17.66f, 11f, 18.99f, 9.66f, 18.99f, 8f)
                curveTo(18.99f, 6.34f, 17.66f, 5f, 16f, 5f)
                curveTo(14.34f, 5f, 13f, 6.34f, 13f, 8f)
                curveTo(13f, 9.66f, 14.34f, 11f, 16f, 11f)
                close()
                moveTo(8f, 11f)
                curveTo(9.66f, 11f, 10.99f, 9.66f, 10.99f, 8f)
                curveTo(10.99f, 6.34f, 9.66f, 5f, 8f, 5f)
                curveTo(6.34f, 5f, 5f, 6.34f, 5f, 8f)
                curveTo(5f, 9.66f, 6.34f, 11f, 8f, 11f)
                close()
                moveTo(8f, 13f)
                curveTo(5.67f, 13f, 1f, 14.17f, 1f, 16.5f)
                verticalLineTo(19f)
                horizontalLineTo(15f)
                verticalLineTo(16.5f)
                curveTo(15f, 14.17f, 10.33f, 13f, 8f, 13f)
                close()
                moveTo(16f, 13f)
                curveTo(15.71f, 13f, 15.38f, 13.02f, 15.03f, 13.05f)
                curveTo(16.19f, 13.89f, 17f, 15.02f, 17f, 16.5f)
                verticalLineTo(19f)
                horizontalLineTo(23f)
                verticalLineTo(16.5f)
                curveTo(23f, 14.17f, 18.33f, 13f, 16f, 13f)
                close()
            }
        }.build()
    }

    val Navigation: ImageVector by lazy {
        ImageVector.Builder(
            name = "Navigation",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                lineTo(4.5f, 20.29f)
                lineTo(5.21f, 21f)
                lineTo(12f, 18f)
                lineTo(18.79f, 21f)
                lineTo(19.5f, 20.29f)
                close()
            }
        }.build()
    }

    val Stop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(6f, 6f)
                horizontalLineTo(18f)
                verticalLineTo(18f)
                horizontalLineTo(6f)
                close()
            }
        }.build()
    }

    val School: ImageVector by lazy {
        ImageVector.Builder(
            name = "School",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(5f, 13.18f)
                verticalLineTo(17.18f)
                lineTo(12f, 21f)
                lineTo(19f, 17.18f)
                verticalLineTo(13.18f)
                lineTo(12f, 17f)
                lineTo(5f, 13.18f)
                close()
                moveTo(12f, 3f)
                lineTo(1f, 9f)
                lineTo(12f, 15f)
                lineTo(21f, 10.09f)
                verticalLineTo(17f)
                horizontalLineTo(23f)
                verticalLineTo(9f)
                lineTo(12f, 3f)
                close()
            }
        }.build()
    }

    val SwapVert: ImageVector by lazy {
        ImageVector.Builder(
            name = "SwapVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 17.01f)
                verticalLineTo(10f)
                horizontalLineTo(14f)
                verticalLineTo(17.01f)
                horizontalLineTo(11f)
                lineTo(15f, 21f)
                lineTo(19f, 17.01f)
                horizontalLineTo(16f)
                close()
                moveTo(9f, 3f)
                lineTo(5f, 6.99f)
                horizontalLineTo(8f)
                verticalLineTo(14f)
                horizontalLineTo(10f)
                verticalLineTo(6.99f)
                horizontalLineTo(13f)
                lineTo(9f, 3f)
                close()
            }
        }.build()
    }

    val Warning: ImageVector by lazy {
        ImageVector.Builder(
            name = "Warning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(1f, 21f)
                horizontalLineTo(23f)
                lineTo(12f, 2f)
                lineTo(1f, 21f)
                close()
                moveTo(13f, 18f)
                horizontalLineTo(11f)
                verticalLineTo(16f)
                horizontalLineTo(13f)
                verticalLineTo(18f)
                close()
                moveTo(13f, 14f)
                horizontalLineTo(11f)
                verticalLineTo(10f)
                horizontalLineTo(13f)
                verticalLineTo(14f)
                close()
            }
        }.build()
    }

    val NotificationsActive: ImageVector by lazy {
        ImageVector.Builder(
            name = "NotificationsActive",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 22f)
                curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
                horizontalLineTo(10f)
                curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
                close()
                moveTo(18f, 16f)
                verticalLineTo(11f)
                curveTo(18f, 7.93f, 16.36f, 5.36f, 13.5f, 4.68f)
                verticalLineTo(4f)
                curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
                curveTo(11.17f, 2.5f, 10.5f, 3.17f, 10.5f, 4f)
                verticalLineTo(4.68f)
                curveTo(7.63f, 5.36f, 6f, 7.92f, 6f, 11f)
                verticalLineTo(16f)
                lineTo(4f, 18f)
                verticalLineTo(19f)
                horizontalLineTo(20f)
                verticalLineTo(18f)
                lineTo(18f, 16f)
                close()
            }
        }.build()
    }

    val NotificationsNone: ImageVector by lazy {
        ImageVector.Builder(
            name = "NotificationsNone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 22f)
                curveTo(13.1f, 22f, 14f, 21.1f, 14f, 20f)
                horizontalLineTo(10f)
                curveTo(10f, 21.1f, 10.9f, 22f, 12f, 22f)
                close()
                moveTo(18f, 16f)
                verticalLineTo(11f)
                curveTo(18f, 7.93f, 16.37f, 5.36f, 13.5f, 4.68f)
                verticalLineTo(4f)
                curveTo(13.5f, 3.17f, 12.83f, 2.5f, 12f, 2.5f)
                curveTo(11.17f, 2.5f, 10.5f, 3.17f, 10.5f, 4f)
                verticalLineTo(4.68f)
                curveTo(7.64f, 5.36f, 6f, 7.92f, 6f, 11f)
                verticalLineTo(16f)
                lineTo(4f, 18f)
                verticalLineTo(19f)
                horizontalLineTo(20f)
                verticalLineTo(18f)
                lineTo(18f, 16f)
                close()
                moveTo(16f, 17f)
                horizontalLineTo(8f)
                verticalLineTo(11f)
                curveTo(8f, 8.52f, 9.51f, 6.5f, 12f, 6.5f)
                curveTo(14.49f, 6.5f, 16f, 8.52f, 16f, 11f)
                verticalLineTo(17f)
                close()
            }
        }.build()
    }

    val Radar: ImageVector by lazy {
        ImageVector.Builder(
            name = "Radar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 20f)
                curveTo(7.59f, 20f, 4f, 16.41f, 4f, 12f)
                curveTo(4f, 7.59f, 7.59f, 4f, 12f, 4f)
                curveTo(16.41f, 4f, 20f, 7.59f, 20f, 12f)
                curveTo(20f, 16.41f, 16.41f, 20f, 12f, 20f)
                close()
                moveTo(12f, 7f)
                curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
                curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
                curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f)
                curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f)
                close()
                moveTo(12f, 14f)
                curveTo(10.9f, 14f, 10f, 13.1f, 10f, 12f)
                curveTo(10f, 10.9f, 10.9f, 10f, 12f, 10f)
                curveTo(13.1f, 10f, 14f, 10.9f, 14f, 12f)
                curveTo(14f, 13.1f, 13.1f, 14f, 12f, 14f)
                close()
            }
        }.build()
    }

    val Vibration: ImageVector by lazy {
        ImageVector.Builder(
            name = "Vibration",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(0f, 15f)
                horizontalLineTo(2f)
                verticalLineTo(9f)
                horizontalLineTo(0f)
                verticalLineTo(15f)
                close()
                moveTo(3f, 17f)
                horizontalLineTo(5f)
                verticalLineTo(7f)
                horizontalLineTo(3f)
                verticalLineTo(17f)
                close()
                moveTo(22f, 9f)
                verticalLineTo(15f)
                horizontalLineTo(24f)
                verticalLineTo(9f)
                horizontalLineTo(22f)
                close()
                moveTo(19f, 17f)
                horizontalLineTo(21f)
                verticalLineTo(7f)
                horizontalLineTo(19f)
                verticalLineTo(17f)
                close()
                moveTo(16.5f, 3f)
                horizontalLineTo(7.5f)
                curveTo(6.67f, 3f, 6f, 3.67f, 6f, 4.5f)
                verticalLineTo(19.5f)
                curveTo(6f, 20.33f, 6.67f, 21f, 7.5f, 21f)
                horizontalLineTo(16.5f)
                curveTo(17.33f, 21f, 18f, 20.33f, 18f, 19.5f)
                verticalLineTo(4.5f)
                curveTo(18f, 3.67f, 17.33f, 3f, 16.5f, 3f)
                close()
                moveTo(16f, 19f)
                horizontalLineTo(8f)
                verticalLineTo(5f)
                horizontalLineTo(16f)
                verticalLineTo(19f)
                close()
            }
        }.build()
    }

    val DarkMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "DarkMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 3f)
                curveTo(7.03f, 3f, 3f, 7.03f, 3f, 12f)
                curveTo(3f, 16.97f, 7.03f, 21f, 12f, 21f)
                curveTo(16.97f, 21f, 21f, 16.97f, 21f, 12f)
                curveTo(21f, 11.54f, 20.96f, 11.08f, 20.9f, 10.63f)
                curveTo(19.78f, 11.49f, 18.38f, 12f, 16.86f, 12f)
                curveTo(12.78f, 12f, 9.47f, 8.69f, 9.47f, 4.61f)
                curveTo(9.47f, 3.99f, 9.55f, 3.39f, 9.71f, 2.82f)
                curveTo(10.45f, 2.93f, 11.23f, 3f, 12f, 3f)
                close()
            }
        }.build()
    }

    val LightMode: ImageVector by lazy {
        ImageVector.Builder(
            name = "LightMode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(12f, 7f)
                curveTo(9.24f, 7f, 7f, 9.24f, 7f, 12f)
                curveTo(7f, 14.76f, 9.24f, 17f, 12f, 17f)
                curveTo(14.76f, 17f, 17f, 14.76f, 17f, 12f)
                curveTo(17f, 9.24f, 14.76f, 7f, 12f, 7f)
                close()
                moveTo(12f, 2f)
                horizontalLineTo(11f)
                verticalLineTo(5f)
                horizontalLineTo(13f)
                verticalLineTo(2f)
                horizontalLineTo(12f)
                close()
                moveTo(12f, 19f)
                horizontalLineTo(11f)
                verticalLineTo(22f)
                horizontalLineTo(13f)
                verticalLineTo(19f)
                horizontalLineTo(12f)
                close()
                moveTo(2f, 12f)
                verticalLineTo(11f)
                horizontalLineTo(5f)
                verticalLineTo(13f)
                horizontalLineTo(2f)
                verticalLineTo(12f)
                close()
                moveTo(19f, 12f)
                verticalLineTo(11f)
                horizontalLineTo(22f)
                verticalLineTo(13f)
                horizontalLineTo(19f)
                verticalLineTo(12f)
                close()
            }
        }.build()
    }

    val Smartphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Smartphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(17f, 1.01f)
                lineTo(7f, 1f)
                curveTo(5.9f, 1f, 5f, 1.9f, 5f, 3f)
                verticalLineTo(21f)
                curveTo(5f, 22.1f, 5.9f, 23f, 7f, 23f)
                horizontalLineTo(17f)
                curveTo(18.1f, 23f, 19f, 22.1f, 19f, 21f)
                verticalLineTo(3f)
                curveTo(19f, 1.9f, 18.1f, 1.01f, 17f, 1.01f)
                close()
                moveTo(17f, 19f)
                horizontalLineTo(7f)
                verticalLineTo(5f)
                horizontalLineTo(17f)
                verticalLineTo(19f)
                close()
            }
        }.build()
    }

    val Restaurant: ImageVector by lazy {
        ImageVector.Builder(
            name = "Restaurant",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(11f, 9f)
                horizontalLineTo(9f)
                verticalLineTo(2f)
                horizontalLineTo(7f)
                verticalLineTo(9f)
                horizontalLineTo(5f)
                verticalLineTo(2f)
                horizontalLineTo(3f)
                verticalLineTo(9f)
                curveTo(3f, 11.21f, 4.79f, 13f, 7f, 13f)
                verticalLineTo(21f)
                horizontalLineTo(9f)
                verticalLineTo(13f)
                curveTo(11.21f, 13f, 13f, 11.21f, 13f, 9f)
                verticalLineTo(2f)
                horizontalLineTo(11f)
                verticalLineTo(9f)
                close()
                moveTo(16f, 6f)
                verticalLineTo(14f)
                horizontalLineTo(18f)
                verticalLineTo(21f)
                horizontalLineTo(20f)
                verticalLineTo(2f)
                curveTo(17.79f, 2f, 16f, 3.79f, 16f, 6f)
                close()
            }
        }.build()
    }

    val People: ImageVector by lazy {
        ImageVector.Builder(
            name = "People",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16f, 11f)
                curveTo(17.66f, 11f, 18.99f, 9.66f, 18.99f, 8f)
                curveTo(18.99f, 6.34f, 17.66f, 5f, 16f, 5f)
                curveTo(14.34f, 5f, 13f, 6.34f, 13f, 8f)
                curveTo(13f, 9.66f, 14.34f, 11f, 16f, 11f)
                close()
                moveTo(8f, 11f)
                curveTo(9.66f, 11f, 10.99f, 9.66f, 10.99f, 8f)
                curveTo(10.99f, 6.34f, 9.66f, 5f, 8f, 5f)
                curveTo(6.34f, 5f, 5f, 6.34f, 5f, 8f)
                curveTo(5f, 9.66f, 6.34f, 11f, 8f, 11f)
                close()
                moveTo(8f, 13f)
                curveTo(5.67f, 13f, 1f, 14.17f, 1f, 16.5f)
                verticalLineTo(19f)
                horizontalLineTo(15f)
                verticalLineTo(16.5f)
                curveTo(15f, 14.17f, 10.33f, 13f, 8f, 13f)
                close()
                moveTo(16f, 13f)
                curveTo(15.71f, 13f, 15.38f, 13.02f, 15.03f, 13.05f)
                curveTo(16.19f, 13.89f, 17f, 15.02f, 17f, 16.5f)
                verticalLineTo(19f)
                horizontalLineTo(23f)
                verticalLineTo(16.5f)
                curveTo(23f, 14.17f, 18.33f, 13f, 16f, 13f)
                close()
            }
        }.build()
    }

    val OpenInNew: ImageVector by lazy {
        ImageVector.Builder(
            name = "OpenInNew",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 19f)
                horizontalLineTo(5f)
                verticalLineTo(5f)
                horizontalLineTo(12f)
                verticalLineTo(3f)
                horizontalLineTo(5f)
                curveTo(3.89f, 3f, 3f, 3.9f, 3f, 5f)
                verticalLineTo(19f)
                curveTo(3f, 20.1f, 3.89f, 21f, 5f, 21f)
                horizontalLineTo(19f)
                curveTo(20.1f, 21f, 21f, 20.1f, 21f, 19f)
                verticalLineTo(12f)
                horizontalLineTo(19f)
                verticalLineTo(19f)
                close()
                moveTo(14f, 3f)
                verticalLineTo(5f)
                horizontalLineTo(17.59f)
                lineTo(7.76f, 14.83f)
                lineTo(9.17f, 16.24f)
                lineTo(19f, 6.41f)
                verticalLineTo(10f)
                horizontalLineTo(21f)
                verticalLineTo(3f)
                horizontalLineTo(14f)
                close()
            }
        }.build()
    }
}
