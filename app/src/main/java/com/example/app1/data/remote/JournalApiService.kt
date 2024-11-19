package com.example.app1.data.remote

import com.example.app1.data.model.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface JournalApiService {


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

    data class JournalRequest(
        val journalId: String,
        val userId: String,
        val title: String,
        val content: String,
        val mood: Int,
        val date: Long,
        val isEdited: Boolean,
        val isDeleted: Boolean
    )

    // User-related endpoints
    @Multipart
    @POST("user/register")
    suspend fun registerUser(
        @Part image: MultipartBody.Part?,
        @Part("username") username: RequestBody,
        @Part("name") name: RequestBody,
        @Part("lastname") lastname: RequestBody,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody
    ): Response<User>
    @POST("user/login")
    suspend fun loginUser(@Body credentials: Map<String, String>): Response<User>

    @GET("user/{userId}")
    suspend fun getUserById(@Path("userId") userId: String): Response<User>

    @PUT("user/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body user: User): Response<User>

    @Multipart
    @PUT("user/{userId}/profile_picture")
    suspend fun updateUserProfileImage(
        @Path("userId") userId: String,
        @Part profilePicture: MultipartBody.Part
    ): Response<User>

    @PUT("user/{userId}/mark_deleted")
    suspend fun userDeletedOnCloud(@Path("userId") userId: String): Response<User>

    // JournalEntry-related endpoints


    @POST("journal/create")
    suspend fun registerJournalEntry(
        @Body journalRequest: JournalRequest
    ): Response<JournalEntry>

    @FormUrlEncoded
    @PUT("journal/{entryId}/update")
    suspend fun updateJournalEntry(
        @Path("entryId") entryId: String,
        @Field("title") title: String,
        @Field("content") content: String,
        @Field("date") date: Long,
        @Field("isDeleted") isDeleted: Boolean
    ): Response<JournalEntry>

    @GET("journal/{entryId}")
    suspend fun getJournalEntryById(@Path("entryId") entryId: String): Response<JournalEntry>

    @GET("journals/{userId}")
    suspend fun getAllJournalEntries(
        @Path("userId") userId: String
    ): Response<List<JournalEntry>>

    @PUT("journal/{entryId}")
    suspend fun markJournalEntryAsDeleted(
        @Path("entryId") entryId: String
    ): Response<JournalEntry>


    // Image-related endpoints
    @POST("entries/{entryId}/images")
    suspend fun addImageToEntry(@Path("entryId") entryId: String, @Body image: Image): Response<Image>

    @GET("image/{journalId}")
    suspend fun getImagesByEntryId(
        @Path("journalId") journalId: String
    ): Response<List<Image>>

    @PUT("image/{imageId}/delete")
    suspend fun markImageAsDeleted(
        @Path("imageId") imageId: String
    ): Response<Image>

    @DELETE("image/{imageId}")
    suspend fun deleteImage(
        @Path("imageId") imageId: String
    ): Response<Void>



    @Multipart
    @POST("journals/{journalId}/images")
    suspend fun uploadJournalImages(
        @Path("journalId") journalId: RequestBody,
        @Part images: List<MultipartBody.Part>
    ): Response<Unit>

    // Reminder-related endpoints

    @POST("reminder/create")
    suspend fun addReminder(@Body reminder: Reminder): Response<Reminder>

    @GET("reminder/user/{userId}")
    suspend fun getRemindersByUser(@Path("userId") userId: String): Response<List<Reminder>>

    @PUT("reminder/{reminderId}")
    suspend fun updateReminder(
        @Path("reminderId") reminderId: String,
        @Body reminder: Reminder
    ): Response<Reminder>

    @DELETE("reminder/{reminderId}")
    suspend fun deleteReminder(@Path("reminderId") reminderId: String): Response<Void>

    @PUT("reminder/{reminderId}/mark_deleted")
    suspend fun markReminderDeleted(@Path("reminderId") reminderId: String): Response<Reminder>
}