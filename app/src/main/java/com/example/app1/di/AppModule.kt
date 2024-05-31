package com.example.app1.di

import android.content.Context
import androidx.room.Room
import android.app.Application
import com.example.app1.data.local.JournalDatabase
import com.example.app1.data.local.JournalEntryDao
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.repository.JournalRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(app: Application): JournalDatabase =
        Room.databaseBuilder(app, JournalDatabase::class.java, "journal_database")
            .build()

    @Provides
    fun provideJournalEntryDao(database: JournalDatabase): JournalEntryDao =
        database.journalEntryDao()

    @Provides
    @Singleton
    fun provideRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl("https://api.example.com/")
        .client(OkHttpClient.Builder().build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideJournalApiService(retrofit: Retrofit): JournalApiService =
        retrofit.create(JournalApiService::class.java)

    @Provides
    @Singleton
    fun provideJournalRepository(
        journalEntryDao: JournalEntryDao,
        journalApiService: JournalApiService
    ): JournalRepository = JournalRepository(journalEntryDao, journalApiService)
}