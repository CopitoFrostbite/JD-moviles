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

    @POST("user/update-password")
    suspend fun updatePassword(@Body passwordData: Map<String, String>): Response<Unit>

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

    @PUT("/journals/{journalId}")
    suspend fun updateJournalEntry(
        @Path("journalId") journalId: String,
        @Body journalRequest: JournalRequest
    ): Response<JournalEntry>

    @PUT("/journals/{journalId}/delete")
    suspend fun updateJournalDeleteFlag(
        @Path("journalId") journalId: String,
        @Query("isDeleted") isDeleted: Boolean
    ): Response<Unit>



    @GET("journals/{userId}")
    suspend fun getAllJournalEntries(
        @Path("userId") userId: String
    ): Response<List<JournalEntry>>


    // Image-related endpoints
    @Multipart
    @POST("entries/{entryId}/images")
    suspend fun addImageToEntry(
        @Part("entryId") entryId: RequestBody,
        @Part imagePart: MultipartBody.Part,
        @Part("imageId") imageId: RequestBody,
        @Part("filePath") filePath: RequestBody,
        @Part("dateAdded") dateAdded: RequestBody,
        @Part("syncDate") syncDate: RequestBody
    ): Response<Void>

    @GET("image/{journalId}")
    suspend fun getImagesByUserId(
        @Path("journalId") journalId: String
    ): Response<List<Image>>

    @PUT("image/{imageId}/delete")
    suspend fun markImageAsDeleted(
        @Path("imageId") imageId: String
    ): Response<Image>




}