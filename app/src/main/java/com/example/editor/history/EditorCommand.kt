package com.example.editor.history

import com.example.data.model.ShapeObject

sealed class EditorCommand {
    data class AddShape(val shape: ShapeObject) : EditorCommand()
    data class DeleteShapes(val deletedShapes: List<ShapeObject>) : EditorCommand()
    data class ModifyShapes(val oldShapes: List<ShapeObject>, val newShapes: List<ShapeObject>) : EditorCommand()
    data class ReorderShapes(val oldList: List<ShapeObject>, val newList: List<ShapeObject>) : EditorCommand()
    data class ReplaceAllShapes(val oldList: List<ShapeObject>, val newList: List<ShapeObject>) : EditorCommand()
}
