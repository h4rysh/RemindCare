package com.aistudio.remindcare.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aistudio.remindcare.ui.components.CaregiverPinPadDialog
import com.aistudio.remindcare.ui.theme.RemindCareTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel5)
class CaregiverPinPadScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testCaregiverPinPadDefaultVisualHierarchy() {
        composeTestRule.setContent {
            RemindCareTheme(darkTheme = false) {
                CaregiverPinPadDialog(
                    title = "Caregiver Authorization",
                    subtitle = "Enter your 4-digit security PIN to continue.",
                    isError = false,
                    onPinSubmit = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/caregiver_pin_pad_default.png")
    }

    @Test
    fun testCaregiverPinPadErrorStateVisualHierarchy() {
        composeTestRule.setContent {
            RemindCareTheme(darkTheme = false) {
                CaregiverPinPadDialog(
                    title = "Caregiver Authorization",
                    subtitle = "Enter your 4-digit security PIN to continue.",
                    isError = true,
                    errorMessage = "Incorrect PIN. Please try again.",
                    onPinSubmit = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("src/test/screenshots/caregiver_pin_pad_error.png")
    }
}
