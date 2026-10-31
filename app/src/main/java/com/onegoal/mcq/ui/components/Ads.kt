package com.onegoal.mcq.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.onegoal.mcq.ads.AdUnits

enum class BannerSize(val adSize: AdSize) {
    /** 320x50 strip used on lists and in the PDF viewer. */
    Banner(AdSize.BANNER),

    /** 300x250 box shown on the home screen. */
    MediumRectangle(AdSize.MEDIUM_RECTANGLE),
}

/** AdMob banner. Uses Google's test unit unless the admin panel provides one. */
@Composable
fun BannerAd(
    enabled: Boolean,
    adUnitId: String?,
    size: BannerSize,
    modifier: Modifier = Modifier,
) {
    if (!enabled || LocalInspectionMode.current) return
    val context = LocalContext.current
    val unitId = adUnitId ?: AdUnits.TEST_BANNER
    val adView = remember(unitId, size) {
        AdView(context).apply {
            setAdSize(size.adSize)
            this.adUnitId = unitId
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AndroidView(factory = { adView })
    }
}
