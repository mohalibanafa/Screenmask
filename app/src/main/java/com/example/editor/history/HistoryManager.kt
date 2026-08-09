package com.example.editor.history

import com.example.data.model.ShapeObject
import java.util.ArrayDeque

class HistoryManager(private val maxHistorySize: Int = 50) {

    private val undoStack = ArrayDeque<EditorCommand>()
    private val redoStack = ArrayDeque<EditorCommand>()

    fun pushCommand(command: EditorCommand) {
        undoStack.push(command)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeLast()
        }
        redoStack.clear()
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()
    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun undo(currentShapes: List<ShapeObject>): List<ShapeObject>? {
        if (!canUndo()) return null
        val command = undoStack.pop()
        redoStack.push(command)

        return when (command) {
            is EditorCommand.AddShape -> {
                currentShapes.filterNot { it.id == command.shape.id }
            }
            is EditorCommand.DeleteShapes -> {
                val currentIds = currentShapes.map { it.id }.toSet()
                val restored = currentShapes.toMutableList()
                command.deletedShapes.forEach { deleted ->
                    if (deleted.id !in currentIds) {
                        restored.add(deleted)
                    }
                }
                restored.sortedBy { it.zIndex }
            }
            is EditorCommand.ModifyShapes -> {
                val oldMap = command.oldShapes.associateBy { it.id }
                currentShapes.map { oldMap[it.id] ?: it }
            }
            is EditorCommand.ReorderShapes -> {
                command.oldList
            }
            is EditorCommand.ReplaceAllShapes -> {
                command.oldList
            }
        }
    }

    fun redo(currentShapes: List<ShapeObject>): List<ShapeObject>? {
        if (!canRedo()) return null
        val command = redoStack.pop()
        undoStack.push(command)

        return when (command) {
            is EditorCommand.AddShape -> {
                if (currentShapes.none { it.id == command.shape.id }) {
                    (currentShapes + command.shape).sortedBy { it.zIndex }
                } else currentShapes
            }
            is EditorCommand.DeleteShapes -> {
                val deletedIds = command.deletedShapes.map { it.id }.toSet()
                currentShapes.filterNot { it.id in deletedIds }
            }
            is EditorCommand.ModifyShapes -> {
                val newMap = command.newShapes.associateBy { it.id }
                currentShapes.map { newMap[it.id] ?: it }
            }
            is EditorCommand.ReorderShapes -> {
                command.newList
            }
            is EditorCommand.ReplaceAllShapes -> {
                command.newList
            }
        }
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
