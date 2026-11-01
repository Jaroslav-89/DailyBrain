package com.jaroapps.dailybrain.feature.avatar.domain.model

enum class AvatarCosmeticType {
    COSTUME,
    BACKGROUND,
}

data class AvatarCosmeticItem(
    val id: String,
    val displayName: String,
    val assetPath: String,
    val type: AvatarCosmeticType,
    val price: Int = 100,
)

data class OwnedAvatarCosmetic(
    val type: AvatarCosmeticType,
    val itemId: String,
)

object AvatarCosmeticCatalog {
    val costumes = listOf(
        AvatarCosmeticItem("spacesuit", "Скафандр", "avatar/costumes/01_spacesuit.svg", AvatarCosmeticType.COSTUME),
        AvatarCosmeticItem("matryoshka", "Матрёшка", "avatar/costumes/02_matryoshka.svg", AvatarCosmeticType.COSTUME),
        AvatarCosmeticItem("square_sponge", "Морская губка", "avatar/costumes/03_square_sponge.svg", AvatarCosmeticType.COSTUME),
        AvatarCosmeticItem("croissant", "Круассан", "avatar/costumes/04_croissant.svg", AvatarCosmeticType.COSTUME),
        AvatarCosmeticItem("bear", "Медведь", "avatar/costumes/05_bear.svg", AvatarCosmeticType.COSTUME),
    )

    val backgrounds = listOf(
        AvatarCosmeticItem("pyramids", "Пирамиды", "avatar/backgrounds/01_pyramids.svg", AvatarCosmeticType.BACKGROUND),
        AvatarCosmeticItem("forest", "Лес", "avatar/backgrounds/02_forest.svg", AvatarCosmeticType.BACKGROUND),
        AvatarCosmeticItem("beach", "Пляж", "avatar/backgrounds/03_beach.svg", AvatarCosmeticType.BACKGROUND),
        AvatarCosmeticItem("office", "Офис", "avatar/backgrounds/04_office.svg", AvatarCosmeticType.BACKGROUND),
        AvatarCosmeticItem("snow_mountains", "Снежные горы", "avatar/backgrounds/05_snow_mountains.svg", AvatarCosmeticType.BACKGROUND),
    )

    fun find(type: AvatarCosmeticType, itemId: String): AvatarCosmeticItem? =
        (if (type == AvatarCosmeticType.COSTUME) costumes else backgrounds)
            .firstOrNull { it.id == itemId }

    fun findAny(itemId: String): AvatarCosmeticItem? =
        costumes.firstOrNull { it.id == itemId } ?: backgrounds.firstOrNull { it.id == itemId }
}
