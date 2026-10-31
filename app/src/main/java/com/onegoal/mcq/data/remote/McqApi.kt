package com.onegoal.mcq.data.remote

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/** Endpoints of the Laravel backend (routes/api.php). */
interface McqApi {
    @GET("api/app-config")
    suspend fun appConfig(@Query("package_name") packageName: String): ObjectResponse<AppConfigDto>

    @FormUrlEncoded
    @POST("api/mcq-categories")
    suspend fun categories(@Field("package_name") packageName: String): ListResponse<CategoryDto>

    @FormUrlEncoded
    @POST("api/mcq-sub-categories")
    suspend fun subCategories(@Field("mcq_category_id") categoryId: Long): ListResponse<SubCategoryDto>

    @FormUrlEncoded
    @POST("api/mcq-sub-sub-categories")
    suspend fun subSubCategories(@Field("mcq_sub_category_id") subCategoryId: Long): ListResponse<SubSubCategoryDto>
}
