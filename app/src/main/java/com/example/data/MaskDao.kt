package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MaskDao {
    @Query("SELECT * FROM masks ORDER BY updatedAt ASC")
    fun getAllMasks(): Flow<List<MaskEntity>>

    @Query("SELECT * FROM masks WHERE id = :id")
    suspend fun getMaskById(id: String): MaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMask(mask: MaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMasks(masks: List<MaskEntity>)

    @Update
    suspend fun updateMask(mask: MaskEntity)

    @Query("DELETE FROM masks WHERE id = :id")
    suspend fun deleteMaskById(id: String)

    @Query("DELETE FROM masks")
    suspend fun deleteAllMasks()

    @Query("UPDATE masks SET isLocked = :isLocked")
    suspend fun updateAllLockState(isLocked: Boolean)

    @Query("UPDATE masks SET isVisible = :isVisible")
    suspend fun updateAllVisibilityState(isVisible: Boolean)
}
