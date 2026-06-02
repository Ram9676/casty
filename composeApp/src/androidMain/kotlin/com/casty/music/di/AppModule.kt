package com.casty.music.di

import android.content.Context
import androidx.room.Room
import com.casty.music.data.DatabaseRepository
import com.casty.music.data.LyricsProvider
import com.casty.music.data.PlayerServiceConnection
import com.casty.music.data.db.CastyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideCastyDatabase(@ApplicationContext context: Context): CastyDatabase {
        val dbName = CastyDatabase.DATABASE_NAME
        return try {
            Room.databaseBuilder(
                context,
                CastyDatabase::class.java,
                dbName
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
        } catch (e: Exception) {
            // This prevents the app from exiting immediately on launch after DB schema
            // changes during development/migrations (common cause of "Room cannot verify
            // the data integrity" or similar on update installs that preserve the old .db file).
            Timber.e(e, "Casty DB open failed (schema mismatch after recent changes?). Deleting DB and recreating to prevent crash.")
            context.deleteDatabase(dbName)
            Room.databaseBuilder(
                context,
                CastyDatabase::class.java,
                dbName
            )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .fallbackToDestructiveMigrationOnDowngrade(dropAllTables = true)
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideDatabaseRepository(
        @ApplicationContext context: Context,
        database: CastyDatabase
    ): DatabaseRepository {
        return DatabaseRepository(context, database)
    }

    @Provides
    @Singleton
    fun provideLyricsProvider(): LyricsProvider {
        return LyricsProvider()
    }
}
