package com.jaroapps.dailybrain.feature.avatar.data

import com.jaroapps.dailybrain.feature.avatar.domain.model.AvatarConfiguration
import org.junit.Assert.assertEquals
import org.junit.Test

class AvatarJsonCodecTest {

    @Test
    fun `new appearance choices survive encode and decode`() {
        val configuration = AvatarConfiguration.Default.copy(
            backgroundId = 10,
            glassesId = 5,
            frecklesId = 4,
            facialHairId = 4,
        )

        assertEquals(
            configuration,
            AvatarJsonCodec.decode(AvatarJsonCodec.encode(configuration)),
        )
    }

    @Test
    fun `older saved avatars default facial hair to none`() {
        val restored = AvatarJsonCodec.decode(
            """{"schemaVersion":3,"glassesId":3,"frecklesId":2}""",
        )

        assertEquals(AvatarConfiguration.NO_FACIAL_HAIR_ID, restored.facialHairId)
        assertEquals(3, restored.glassesId)
        assertEquals(2, restored.frecklesId)
        assertEquals(AvatarConfiguration.CURRENT_SCHEMA_VERSION, restored.schemaVersion)
    }

    @Test
    fun `legacy background colors migrate to equivalent gradients`() {
        val yellow = AvatarJsonCodec.decode(
            """{"schemaVersion":3,"backgroundId":11}""",
        )
        val peach = AvatarJsonCodec.decode(
            """{"schemaVersion":3,"backgroundId":12}""",
        )

        assertEquals(5, yellow.backgroundId)
        assertEquals(2, peach.backgroundId)
    }

    @Test
    fun `invalid new options fall back to supported values`() {
        val restored = AvatarJsonCodec.decode(
            """{"schemaVersion":4,"glassesId":99,"frecklesId":99,"facialHairId":99}""",
        )

        assertEquals(AvatarConfiguration.NO_GLASSES_ID, restored.glassesId)
        assertEquals(AvatarConfiguration.NO_FRECKLES_ID, restored.frecklesId)
        assertEquals(AvatarConfiguration.NO_FACIAL_HAIR_ID, restored.facialHairId)
    }
}
