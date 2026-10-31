package com.onegoal.mcq.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListResponse<T>(
    val status: Boolean = false,
    val message: String? = null,
    val data: List<T> = emptyList(),
)

@Serializable
data class ObjectResponse<T>(
    val status: Boolean = false,
    val message: String? = null,
    val data: T? = null,
)

@Serializable
data class CategoryDto(
    val id: Long,
    @SerialName("category_name") val name: String,
    @SerialName("is_category") val hasChildren: Int = 0,
    @SerialName("item_label") val itemLabel: String? = null,
    val image: String? = null,
    val pdf: String? = null,
    @SerialName("answer_pdf") val answerPdf: String? = null,
    @SerialName("question_label") val questionLabel: String? = null,
    @SerialName("answer_label") val answerLabel: String? = null,
)

@Serializable
data class SubCategoryDto(
    val id: Long,
    val name: String,
    @SerialName("mcq_category") val categoryName: String? = null,
    @SerialName("item_label") val itemLabel: String? = null,
    @SerialName("is_sub_sub_category") val hasChildren: Int = 0,
    val image: String? = null,
    val pdf: String? = null,
    @SerialName("answer_pdf") val answerPdf: String? = null,
    @SerialName("question_label") val questionLabel: String? = null,
    @SerialName("answer_label") val answerLabel: String? = null,
)

@Serializable
data class SubSubCategoryDto(
    val id: Long,
    val name: String,
    @SerialName("mcq_category") val categoryName: String? = null,
    @SerialName("mcq_sub_category") val subCategoryName: String? = null,
    @SerialName("item_label") val itemLabel: String? = null,
    val image: String? = null,
    val pdf: String? = null,
    @SerialName("answer_pdf") val answerPdf: String? = null,
    @SerialName("question_label") val questionLabel: String? = null,
    @SerialName("answer_label") val answerLabel: String? = null,
)

/** Mirrors GET /api/app-config in the Laravel backend. */
@Serializable
data class AppConfigDto(
    @SerialName("app_name") val appName: String? = null,
    @SerialName("toolbar_title") val toolbarTitle: String? = null,
    @SerialName("splash_title") val splashTitle: String? = null,
    val logo: String? = null,
    @SerialName("splash_image") val splashImage: String? = null,
    @SerialName("feedback_email") val feedbackEmail: String? = null,
    @SerialName("privacy_policy_url") val privacyPolicyUrl: String? = null,
    @SerialName("more_apps_url") val moreAppsUrl: String? = null,
    @SerialName("share_message") val shareMessage: String? = null,
    @SerialName("support_title") val supportTitle: String? = null,
    @SerialName("support_message") val supportMessage: String? = null,
    @SerialName("last_read_title") val lastReadTitle: String? = null,
    @SerialName("last_read_message") val lastReadMessage: String? = null,
    @SerialName("ads_enabled") val adsEnabled: Boolean = true,
    @SerialName("admob_banner_id") val admobBannerId: String? = null,
    @SerialName("admob_interstitial_id") val admobInterstitialId: String? = null,
    @SerialName("interstitial_every") val interstitialEvery: Int = 3,
    @SerialName("min_version_code") val minVersionCode: Int = 1,
)
