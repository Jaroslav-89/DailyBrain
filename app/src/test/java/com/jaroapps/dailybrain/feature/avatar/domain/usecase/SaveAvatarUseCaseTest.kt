package com.jaroapps.dailybrain.feature.avatar.domain.usecase

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import com.jaroapps.dailybrain.feature.avatar.domain.repository.AvatarRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `all newest editor options are accepted for persistence`() = runTest {
        val repository = FakeAvatarRepository()
        val configuration = AvatarConfiguration(
            skinToneId = 5,
            hairStyleId = 5,
            hairColorId = 5,
            eyeStyleId = 5,
            mouthStyleId = 2,
            accessoryId = 2,
            backgroundId = 10,
            faceTypeId = 5,
            eyeColorId = 5,
            clothingStyleId = 5,
            clothingColorId = 11,
            glassesId = 5,
            frecklesId = 4,
            bodyTypeId = 5,
            hairLengthId = 5,
            facialHairId = 4,
        )

        SaveAvatarUseCase(repository)(configuration)

        assertEquals(configuration, repository.saved)
    }

    @Test
    fun `unknown asset id is rejected before persistence`() = runTest {
        val repository = FakeAvatarRepository()
        val useCase = SaveAvatarUseCase(repository)

        var failure: IllegalArgumentException? = null
        try {
            useCase(AvatarConfiguration(hairStyleId = 99))
        } catch (error: IllegalArgumentException) {
            failure = error
        }

        assertNotNull(failure)
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
