package com.example.editor.ui

import com.example.data.model.ShapeType

sealed class EditorTool {
    object Select : EditorTool()
    object MultiSelect : EditorTool()
    data class Draw(val shapeType: ShapeType) : EditorTool()
}
