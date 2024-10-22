package com.example.app1.data.remote

import com.example.app1.data.model.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface JournalApiService {

    // User-related endpoints
    @Multipart
    @POST("user/register")
    suspend fun registerUser(
        @Part avatar: MultipartBody.Part?,
        @Part("username") username: RequestBody,
        @Part("name") name: RequestBody,
        @Part("lastname") lastname: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody
    ): Response<User>


    data class LoginResponse(
        val message: String,
        val user: UserResponse
    )

    data class UserResponse(
        val userId: String,
        val username: String,
        val name: String,
        val lastname: String,
        val email: String,
        val profilePicture: String
    )
    @POST("user/login")
    suspend fun loginUser(@Body credentials: Map<String, String>): Response<User>

    @GET("user/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<User>

    @PUT("user/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: User): Response<User>

    // JournalEntry-related endpoints
    @Multipart
    @POST("journal/create")
    suspend fun registerJournalEntry(

        @Part("userId") userId: RequestBody,
        @Part("title") title: RequestBody,
        @Part("content") content: RequestBody,
        @Part("date") date: RequestBody,
        @Part("isEdited") isEdited: RequestBody
    ): Response<JournalEntry>

    @FormUrlEncoded
    @PUT("journal/{entryId}/update")
    suspend fun updateJournalEntry(
        @Path("entryId") entryId: String,
        @Field("title") title: String,
        @Field("content") content: String,
        @Field("date") date: Long,
        @Field("isEdited") isEdited: Boolean
    ): Response<JournalEntry>

    @GET("journal/")
    suspend fun getJournalEntryById(@Body entryId: String): Response<JournalEntry>

    @GET("journals")
    suspend fun getAllJournalEntries(@Body userId: String): Response<List<JournalEntry>>

    // Image-related endpoints
    @POST("entries/{entryId}/images")
    suspend fun addImageToEntry(@Path("entryId") entryId: String, @Body image: Image): Response<Image>

    @GET("entries/{entryId}/images")
    suspend fun getImagesByEntryId(@Path("entryId") entryId: String): Response<List<Image>>

    // Reminder-related endpoints
    @POST("reminders")
    suspend fun createReminder(@Body reminder: Reminder): Response<Reminder>

    @GET("reminders/user/{userId}")
    suspend fun getRemindersByUserId(@Path("userId") userId: String): Response<List<Reminder>>

    // Settings-related endpoints
    @POST("settings")
    suspend fun updateSettings(@Body settings: Settings): Response<Settings>

    @GET("settings/user/{userId}")
    suspend fun getSettingsByUserId(@Path("userId") userId: String): Response<Settings>
}