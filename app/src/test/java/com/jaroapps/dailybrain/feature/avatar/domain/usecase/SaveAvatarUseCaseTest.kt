package com.jaroapps.dailybrain.feature.avatar.domain.usecase

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SaveAvatarUseCaseTest {
    @Test
    fun `valid configuration is saved through repository`() = runTest {
        val repository = FakeAvatarRepository()
        val configuration = AvatarConfiguration(hairStyleId = 3, accessoryId = 1)

        SaveAvatarUseCase(repository)(configuration)

        assertEquals(configuration, repository.saved)
    }

    @Test
    fun `unknown asset id is rejected before persistence`() = runTest {
        val repository = FakeAvatarRepository()
        val useCase = SaveAvatarUseCase(repository)

        assertThrows(IllegalArgumentException::class.java) {
            runTest { useCase(AvatarConfiguration(hairStyleId = 99)) }
        }
        assertEquals(null, repository.saved)
    }

    private class FakeAvatarRepository : AvatarRepository {
        var saved: AvatarConfiguration? = null

        override fun observeAvatar(): Flow<AvatarConfiguration> =
            flowOf(saved ?: AvatarConfiguration.Default)

        override suspend fun saveAvatar(configuration: AvatarConfiguration) {
            saved = configuration
        }

        override suspend fun resetAvatar() {
            saved = null
        }
    }
}
