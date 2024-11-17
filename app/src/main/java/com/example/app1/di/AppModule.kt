package com.example.app1.di

import android.content.Context
import androidx.room.Room

import com.example.app1.data.local.*
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.repository.*

import com.example.app1.ui.adapters.LongDateTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext appContext: Context): JournalDatabase {
        return Room.databaseBuilder(
            appContext,
            JournalDatabase::class.java,
            "journal_app.db"
        ).build()
    }

    @Singleton
    @Provides
    fun provideUserDao(db: JournalDatabase): UserDao {
        return db.userDao()
    }

    @Singleton
    @Provides
    fun provideJournalEntryDao(db: JournalDatabase): JournalEntryDao {
        return db.journalEntryDao()
    }

    @Singleton
    @Provides
    fun provideImageDao(db: JournalDatabase): ImageDao {
        return db.imageDao()
    }

    @Singleton
    @Provides
    fun provideReminderDao(db: JournalDatabase): ReminderDao {
        return db.reminderDao()
    }

    @Singleton
    @Provides
    fun provideSettingsDao(db: JournalDatabase): SettingsDao {
        return db.settingsDao()
    }

    @Singleton
    @Provides
    fun provideRetrofit(gson: Gson): Retrofit {
        val logging = HttpLoggingInterceptor()
        logging.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl("https://apimoviles-yha6.onrender.com/api/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))  // Usa Gson personalizado
            .build()
    }


    @Singleton
    @Provides
    fun provideApiService(retrofit: Retrofit): JournalApiService {
        return retrofit.create(JournalApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideUserRepository(
        api: JournalApiService,
        userDao: UserDao,
        @ApplicationContext context: Context
    ): UserRepository {
        return UserRepository(api, userDao, context)
    }

    @Singleton
    @Provides
    fun provideJournalEntryRepository(
        api: JournalApiService,
        journalEntryDao: JournalEntryDao,
        @ApplicationContext context: Context
    ): JournalEntryRepository {
        return JournalEntryRepository(api, journalEntryDao, context)
    }

    @Singleton
    @Provides
    fun provideImageRepository(
        api: JournalApiService,
        imageDao: ImageDao,
        @ApplicationContext context: Context
    ): ImageRepository {
        return ImageRepository(api, imageDao, context)
    }

    @Singleton
    @Provides
    fun provideReminderRepository(api: JournalApiService, reminderDao: ReminderDao): ReminderRepository {
        return ReminderRepository(api, reminderDao)
    }

    @Singleton
    @Provides
    fun provideSettingsRepository(api: JournalApiService, settingsDao: SettingsDao): SettingsRepository {
        return SettingsRepository(api, settingsDao)
    }

    @Singleton
    @Provides
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(Long::class.java, LongDateTypeAdapter())  // Adaptador para Long
            .create()
    }


}