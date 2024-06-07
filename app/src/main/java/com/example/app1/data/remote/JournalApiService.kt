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

    @POST("user/login")
    suspend fun loginUser(@Body credentials: Map<String, String>): Response<User>

    @GET("user/{userId}")
    suspend fun getUserById(@Path("userId") userId: Int): Response<User>

    @PUT("user/{id}")
    suspend fun updateUser(@Path("id") id: Int, @Body user: User): Response<User>

    // JournalEntry-related endpoints
    @POST("entries")
    suspend fun createEntry(@Body entry: JournalEntry): Response<JournalEntry>

    @GET("entries")
    suspend fun getEntries(): Response<List<JournalEntry>>

    @GET("entries/user/{userId}")
    suspend fun getEntriesByUserId(@Path("userId") userId: Int): Response<List<JournalEntry>>

    // Image-related endpoints
    @POST("entries/{entryId}/images")
    suspend fun addImageToEntry(@Path("entryId") entryId: Int, @Body image: Image): Response<Image>

    @GET("entries/{entryId}/images")
    suspend fun getImagesByEntryId(@Path("entryId") entryId: Int): Response<List<Image>>

    // Reminder-related endpoints
    @POST("reminders")
    suspend fun createReminder(@Body reminder: Reminder): Response<Reminder>

    @GET("reminders/user/{userId}")
    suspend fun getRemindersByUserId(@Path("userId") userId: Int): Response<List<Reminder>>

    // Settings-related endpoints
    @POST("settings")
    suspend fun updateSettings(@Body settings: Settings): Response<Settings>

    @GET("settings/user/{userId}")
    suspend fun getSettingsByUserId(@Path("userId") userId: Int): Response<Settings>
}