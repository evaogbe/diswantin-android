package io.github.evaogbe.diswantin.ui.tooling

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.content.res.Configuration.UI_MODE_TYPE_NORMAL
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Wallpapers.BLUE_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.GREEN_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.RED_DOMINATED_EXAMPLE
import androidx.compose.ui.tooling.preview.Wallpapers.YELLOW_DOMINATED_EXAMPLE

@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION
)
@Preview(name = "00 - Light")
@Preview(name = "01 - Dark", uiMode = UI_MODE_NIGHT_YES or UI_MODE_TYPE_NORMAL)
@Preview(name = "02 - Red", wallpaper = RED_DOMINATED_EXAMPLE, apiLevel = 31)
@Preview(name = "03 - Blue", wallpaper = BLUE_DOMINATED_EXAMPLE, apiLevel = 31)
@Preview(name = "04 - Green", wallpaper = GREEN_DOMINATED_EXAMPLE, apiLevel = 31)
@Preview(name = "05 - Yellow", wallpaper = YELLOW_DOMINATED_EXAMPLE, apiLevel = 31)
@Preview(name = "06 - Phone", device = "spec:width=411dp,height=891dp", showSystemUi = true)
@Preview(
    name = "07 - Phone - Landscape",
    device = "spec:width=411dp,height=891dp,orientation=landscape,dpi=420",
    showSystemUi = true
)
@Preview(
    name = "08 - Unfolded Foldable",
    device = "spec:width=673dp,height=841dp",
    showSystemUi = true
)
@Preview(
    name = "09 - Tablet",
    device = "spec:width=1280dp,height=800dp,dpi=240",
    showSystemUi = true
)
@Preview(
    name = "10 - Desktop",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    showSystemUi = true
)
@Preview(name = "11 - 85%", fontScale = 0.85f)
@Preview(name = "12 - 100%", fontScale = 1.0f)
@Preview(name = "13 - 115%", fontScale = 1.15f)
@Preview(name = "14 - 130%", fontScale = 1.3f)
@Preview(name = "15 - 150%", fontScale = 1.5f)
@Preview(name = "16 - 180%", fontScale = 1.8f)
@Preview(name = "17 - 200%", fontScale = 2f)
annotation class DevicePreviews
