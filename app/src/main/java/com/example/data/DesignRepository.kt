package com.example.data

import com.example.data.model.DesignProject
import com.example.data.model.ShapeObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DesignRepository private constructor(private val dao: DesignProjectDao) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val shapesListType = Types.newParameterizedType(List::class.java, ShapeObject::class.java)
    private val shapesAdapter = moshi.adapter<List<ShapeObject>>(shapesListType)

    val allProjects: Flow<List<DesignProject>> = dao.getAllProjects().map { entities ->
        entities.map { it.toModel() }
    }

    val activeProject: Flow<DesignProject?> = dao.getActiveProject().map { entity ->
        entity?.toModel()
    }

    suspend fun getProjectById(id: String): DesignProject? {
        return dao.getProjectById(id)?.toModel()
    }

    suspend fun getActiveProjectSync(): DesignProject? {
        return dao.getActiveProjectSync()?.toModel()
    }

    suspend fun saveProject(project: DesignProject) {
        val entity = project.toEntity()
        dao.insertProject(entity)
    }

    suspend fun setActiveProject(id: String) {
        dao.clearActiveProjects()
        dao.setActiveProject(id)
    }

    suspend fun deleteProject(id: String) {
        dao.deleteProjectById(id)
    }

    private fun DesignProjectEntity.toModel(): DesignProject {
        val shapesList = try {
            shapesAdapter.fromJson(shapesJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return DesignProject(
            id = id,
            name = name,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            targetDensity = targetDensity,
            shapes = shapesList,
            isBlackoutMode = isBlackoutMode,
            isActive = isActive,
            updatedAt = updatedAt
        )
    }

    private fun DesignProject.toEntity(): DesignProjectEntity {
        val json = try {
            shapesAdapter.toJson(shapes)
        } catch (e: Exception) {
            "[]"
        }
        return DesignProjectEntity(
            id = id,
            name = name,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
            targetDensity = targetDensity,
            shapesJson = json,
            isBlackoutMode = isBlackoutMode,
            isActive = isActive,
            updatedAt = System.currentTimeMillis()
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: DesignRepository? = null

        fun getInstance(db: AppDatabase): DesignRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = DesignRepository(db.designProjectDao())
                INSTANCE = instance
                instance
            }
        }
    }
}
