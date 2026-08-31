package com.example

import android.view.Surface
import com.example.service.FloatingPositionCalculator
import org.junit.Assert.assertEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testFloatingPositionCalculator_0Degrees_NoChange() {
        val pos = FloatingPositionCalculator.calculateRotatedPosition(
            oldX = 100,
            oldY = 200,
            oldWidth = 1080,
            oldHeight = 2400,
            oldRotation = Surface.ROTATION_0,
            newWidth = 1080,
            newHeight = 2400,
            newRotation = Surface.ROTATION_0,
            iconWidth = 80,
            iconHeight = 80
        )
        assertEquals(100, pos.x)
        assertEquals(200, pos.y)
    }

    @Test
    fun testFloatingPositionCalculator_90Degrees_Clockwise() {
        val pos = FloatingPositionCalculator.calculateRotatedPosition(
            oldX = 100,
            oldY = 200,
            oldWidth = 1080,
            oldHeight = 2400,
            oldRotation = Surface.ROTATION_0,
            newWidth = 2400,
            newHeight = 1080,
            newRotation = Surface.ROTATION_90,
            iconWidth = 80,
            iconHeight = 80
        )
        // newX = oldHeight - oldY - iconHeight = 2400 - 200 - 80 = 2120
        // newY = oldX = 100
        assertEquals(2120, pos.x)
        assertEquals(100, pos.y)
    }

    @Test
    fun testFloatingPositionCalculator_180Degrees() {
        val pos = FloatingPositionCalculator.calculateRotatedPosition(
            oldX = 100,
            oldY = 200,
            oldWidth = 1080,
            oldHeight = 2400,
            oldRotation = Surface.ROTATION_0,
            newWidth = 1080,
            newHeight = 2400,
            newRotation = Surface.ROTATION_180,
            iconWidth = 80,
            iconHeight = 80
        )
        // newX = oldWidth - oldX - iconWidth = 1080 - 100 - 80 = 900
        // newY = oldHeight - oldY - iconHeight = 2400 - 200 - 80 = 2120
        assertEquals(900, pos.x)
        assertEquals(2120, pos.y)
    }

    @Test
    fun testFloatingPositionCalculator_270Degrees_CounterClockwise() {
        val pos = FloatingPositionCalculator.calculateRotatedPosition(
            oldX = 100,
            oldY = 200,
            oldWidth = 1080,
            oldHeight = 2400,
            oldRotation = Surface.ROTATION_0,
            newWidth = 2400,
            newHeight = 1080,
            newRotation = Surface.ROTATION_270,
            iconWidth = 80,
            iconHeight = 80
        )
        // newX = oldY = 200
        // newY = oldWidth - oldX - iconWidth = 1080 - 100 - 80 = 900
        assertEquals(200, pos.x)
        assertEquals(900, pos.y)
    }

    @Test
    fun testFloatingPositionCalculator_Full360Cycle() {
        var x = 150
        var y = 300
        val iconW = 60
        val iconH = 60

        // Step 1: 0 -> 90
        var pos = FloatingPositionCalculator.calculateRotatedPosition(
            oldX = x, oldY = y,
            oldWidth = 1080, oldHeight = 2400, oldRotation = Surface.ROTATION_0,
            newWidth = 2400, newHeight = 1080, newRotation = Surface.ROTATION_90,
            iconWidth = iconW, iconHeight = iconH
        )
        assertEquals(2400 - 300 - 60, pos.x) // 2040
        assertEquals(150, pos.y)

        // Step 2: 90 -> 180
        pos = FloatingPositionCalculator.calculateRotatedPosition(
            oldX = pos.x, oldY = pos.y,
            oldWidth = 2400, oldHeight = 1080, oldRotation = Surface.ROTATION_90,
            newWidth = 1080, newHeight = 2400, newRotation = Surface.ROTATION_180,
            iconWidth = iconW, iconHeight = iconH
        )
        assertEquals(1080 - 150 - 60, pos.x) // 870
        assertEquals(2040, pos.y)

        // Step 3: 180 -> 270
        pos = FloatingPositionCalculator.calculateRotatedPosition(
            oldX = pos.x, oldY = pos.y,
            oldWidth = 1080, oldHeight = 2400, oldRotation = Surface.ROTATION_180,
            newWidth = 2400, newHeight = 1080, newRotation = Surface.ROTATION_270,
            iconWidth = iconW, iconHeight = iconH
        )
        assertEquals(2400 - 2040 - 60, pos.x) // 300
        assertEquals(870, pos.y)

        // Step 4: 270 -> 0
        pos = FloatingPositionCalculator.calculateRotatedPosition(
            oldX = pos.x, oldY = pos.y,
            oldWidth = 2400, oldHeight = 1080, oldRotation = Surface.ROTATION_270,
            newWidth = 1080, newHeight = 2400, newRotation = Surface.ROTATION_0,
            iconWidth = iconW, iconHeight = iconH
        )
        // Returns to original x=150, y=300!
        assertEquals(150, pos.x)
        assertEquals(300, pos.y)
    }

    @Test
    fun testPhysicalPixelTransformer_RoundTripBijection() {
        val w0 = 1080f
        val h0 = 2400f
        val origX = 250f
        val origY = 600f

        val rotations = listOf(
            Surface.ROTATION_0,
            Surface.ROTATION_90,
            Surface.ROTATION_180,
            Surface.ROTATION_270
        )

        for (rot in rotations) {
            val winPoint = com.example.service.PhysicalPixelTransformer.physicalToWindow(origX, origY, w0, h0, rot)
            val recovered = com.example.service.PhysicalPixelTransformer.windowToPhysical(winPoint.x, winPoint.y, w0, h0, rot)
            assertEquals("Roundtrip failed for rotation $rot on X", origX, recovered.x, 0.001f)
            assertEquals("Roundtrip failed for rotation $rot on Y", origY, recovered.y, 0.001f)
        }
    }

    @Test
    fun testPhysicalPixelTransformer_MaskLayoutPhysicalLocationInvariant() {
        // Physical crack located at X=108px..324px (10%..30% of 1080), Y=480px..960px (20%..40% of 2400)
        val mask = com.example.data.MaskEntity(
            xRatio = 0.10f,
            yRatio = 0.20f,
            widthRatio = 0.20f,
            heightRatio = 0.20f,
            isPhysicalLock = true
        )

        // In ROTATION_0 (Portrait): screen is 1080 x 2400
        val layout0 = com.example.service.PhysicalPixelTransformer.calculateMaskLayout(
            mask, 1080, 2400, Surface.ROTATION_0
        )
        assertEquals(108, layout0.winX)
        assertEquals(480, layout0.winY)
        assertEquals(216, layout0.winWidth)
        assertEquals(480, layout0.winHeight)

        // In ROTATION_90 (Landscape turned left): screen is 2400 x 1080
        val layout90 = com.example.service.PhysicalPixelTransformer.calculateMaskLayout(
            mask, 2400, 1080, Surface.ROTATION_90
        )
        // Physical Y (480..960) -> X in ROTATION_90 = Y = 480
        // Physical X (108..324) -> Y in ROTATION_90 = W0 - X = 1080 - 324 = 756
        assertEquals(480, layout90.winX)
        assertEquals(756, layout90.winY)
        assertEquals(480, layout90.winWidth)
        assertEquals(216, layout90.winHeight)

        // In ROTATION_180 (Upside down): screen is 1080 x 2400
        val layout180 = com.example.service.PhysicalPixelTransformer.calculateMaskLayout(
            mask, 1080, 2400, Surface.ROTATION_180
        )
        assertEquals(756, layout180.winX)
        assertEquals(1440, layout180.winY)
        assertEquals(216, layout180.winWidth)
        assertEquals(480, layout180.winHeight)

        // In ROTATION_270 (Landscape turned right): screen is 2400 x 1080
        val layout270 = com.example.service.PhysicalPixelTransformer.calculateMaskLayout(
            mask, 2400, 1080, Surface.ROTATION_270
        )
        // Physical Y (480..960) -> X in ROTATION_270 = H0 - Y = 2400 - 960 = 1440
        // Physical X (108..324) -> Y in ROTATION_270 = X = 108
        assertEquals(1440, layout270.winX)
        assertEquals(108, layout270.winY)
        assertEquals(480, layout270.winWidth)
        assertEquals(216, layout270.winHeight)
    }
}

