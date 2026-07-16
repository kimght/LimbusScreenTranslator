package com.kimght.LimbusScreenTranslator.feature.detail

import com.kimght.LimbusScreenTranslator.R
import com.kimght.LimbusScreenTranslator.data.install.InstallState
import org.junit.Assert.assertEquals
import org.junit.Test

class InstallStageLabelTest {

    @Test
    fun `maps install states to stage label resources`() {
        assertEquals(R.string.install_stage_downloading, InstallState.Downloading(42).stageLabelRes())
        assertEquals(R.string.install_stage_verifying, InstallState.Verifying.stageLabelRes())
        assertEquals(R.string.install_stage_extracting, InstallState.Extracting.stageLabelRes())
        assertEquals(R.string.install_stage_saving, InstallState.Persisting.stageLabelRes())
        assertEquals(R.string.install_stage_installing, (null as InstallState?).stageLabelRes())
    }
}
