package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.viewmodel.DonorViewModel

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Blood donors manegement system", appName)
  }

  @Test
  fun testViewModelInitialization() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val app = context.applicationContext as android.app.Application
    val viewModel = DonorViewModel(app)
    viewModel.seedSampleKarnatakaDonors()
    val donors = viewModel.allDonors.value
    println("Successfully initialized ViewModel and fetched donors count: ${donors.size}")
  }
}
