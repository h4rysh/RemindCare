package com.aistudio.remindcare

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Unit test verifying testing infrastructure foundation (MockK, Turbine, Truth, Core-Testing).
 */
class ExampleUnitTest {
  @get:Rule
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testInfrastructure_librariesAvailableAndFunctional() = runTest {
    // Verify Truth
    assertThat(2 + 2).isEqualTo(4)

    // Verify MockK
    val mockService = mockk<InfrastructureProbe>()
    every { mockService.ping() } returns "pong"
    assertThat(mockService.ping()).isEqualTo("pong")

    // Verify Turbine
    val flow = flowOf("alpha", "beta")
    flow.test {
      assertThat(awaitItem()).isEqualTo("alpha")
      assertThat(awaitItem()).isEqualTo("beta")
      awaitComplete()
    }
  }

  interface InfrastructureProbe {
    fun ping(): String
  }
}
