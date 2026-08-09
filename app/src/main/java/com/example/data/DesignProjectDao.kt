package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DesignProjectDao {
    @Query("SELECT * FROM design_projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<DesignProjectEntity>>

    @Query("SELECT * FROM design_projects WHERE id = :id")
    suspend fun getProjectById(id: String): DesignProjectEntity?

    @Query("SELECT * FROM design_projects WHERE isActive = 1 LIMIT 1")
    fun getActiveProject(): Flow<DesignProjectEntity?>

    @Query("SELECT * FROM design_projects WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProjectSync(): DesignProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: DesignProjectEntity)

    @Update
    suspend fun updateProject(project: DesignProjectEntity)

    @Query("DELETE FROM design_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("UPDATE design_projects SET isActive = 0")
    suspend fun clearActiveProjects()

    @Query("UPDATE design_projects SET isActive = (id = :id)")
    suspend fun setActiveProject(id: String)
}
