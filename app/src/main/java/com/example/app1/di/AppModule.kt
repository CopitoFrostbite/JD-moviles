package com.example.app1.di

import android.content.Context
import androidx.room.Room

import com.example.app1.data.local.*
import com.example.app1.data.remote.JournalApiService
import com.example.app1.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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
    fun provideRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://apimoviles-yha6.onrender.com/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .client(OkHttpClient.Builder().build())
            .build()
    }

    @Singleton
    @Provides
    fun provideApiService(retrofit: Retrofit): JournalApiService {
        return retrofit.create(JournalApiService::class.java)
    }

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
    fun provideImageRepository(api: JournalApiService, imageDao: ImageDao): ImageRepository {
        return ImageRepository(api, imageDao)
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
}