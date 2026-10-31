package com.onegoal.mcq.ads

import android.app.Activity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/** Loads one interstitial ahead of time and shows it when asked, then preloads the next. */
class InterstitialAds(private val activity: Activity) {
    private var ad: InterstitialAd? = null
    private var loading = false
    private var unitId: String = AdUnits.TEST_INTERSTITIAL

    fun preload(adUnitId: String?) {
        unitId = adUnitId ?: AdUnits.TEST_INTERSTITIAL
        if (ad != null || loading) return
        loading = true
        InterstitialAd.load(
            activity,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    ad = null
                    loading = false
                }
            },
        )
    }

    /** Shows the ad if one is ready and always calls [onFinished] afterwards. */
    fun show(onFinished: () -> Unit) {
        val current = ad
        if (current == null) {
            onFinished()
            preload(unitId)
            return
        }
        ad = null
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onFinished()
                preload(unitId)
            }

            override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                onFinished()
                preload(unitId)
            }
        }
        current.show(activity)
    }
}
