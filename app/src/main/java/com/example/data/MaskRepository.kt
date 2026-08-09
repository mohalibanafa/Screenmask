package com.example.data

import kotlinx.coroutines.flow.Flow

class MaskRepository(private val maskDao: MaskDao) {
    val allMasks: Flow<List<MaskEntity>> = maskDao.getAllMasks()

    suspend fun getMaskById(id: String): MaskEntity? = maskDao.getMaskById(id)

    suspend fun addMask(mask: MaskEntity) = maskDao.insertMask(mask)

    suspend fun addMasks(masks: List<MaskEntity>) = maskDao.insertMasks(masks)

    suspend fun updateMask(mask: MaskEntity) = maskDao.updateMask(mask)

    suspend fun deleteMask(id: String) = maskDao.deleteMaskById(id)

    suspend fun deleteAllMasks() = maskDao.deleteAllMasks()

    suspend fun setAllLockState(isLocked: Boolean) = maskDao.updateAllLockState(isLocked)

    suspend fun setAllVisibilityState(isVisible: Boolean) = maskDao.updateAllVisibilityState(isVisible)

    companion object {
        @Volatile
        private var INSTANCE: MaskRepository? = null

        fun getInstance(db: AppDatabase): MaskRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = MaskRepository(db.maskDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
