package com.onegoal.mcq

import android.content.Context
import com.onegoal.mcq.data.McqRepository
import com.onegoal.mcq.data.local.AppDatabase
import com.onegoal.mcq.data.pdf.PdfFileCache
import com.onegoal.mcq.data.prefs.AppPreferences
import com.onegoal.mcq.data.remote.McqApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Manual dependency container; one instance lives in [McqApplication]. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request()
            chain.proceed(
                if (request.header("Accept") == null) {
                    request.newBuilder().header("Accept", "application/json").build()
                } else {
                    request
                },
            )
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    private val api: McqApi = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(McqApi::class.java)

    val preferences = AppPreferences(appContext, json)

    private val database = AppDatabase.create(appContext)

    val repository = McqRepository(
        api = api,
        bookmarkDao = database.bookmarkDao(),
        preferences = preferences,
        json = json,
        packageName = appContext.packageName,
        defaultQuestionLabel = appContext.getString(R.string.questions_section),
        defaultAnswerLabel = appContext.getString(R.string.answers_section),
    )

    val pdfFileCache = PdfFileCache(appContext, okHttpClient)
}
