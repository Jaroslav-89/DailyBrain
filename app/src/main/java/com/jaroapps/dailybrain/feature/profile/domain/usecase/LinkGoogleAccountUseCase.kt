package com.jaroapps.dailybrain.feature.profile.domain.usecase

import com.jaroapps.dailybrain.feature.profile.domain.model.GoogleAccountProfile
import com.jaroapps.dailybrain.feature.profile.domain.model.LinkGoogleAccountResult
import com.jaroapps.dailybrain.feature.profile.domain.repository.UserProfileRepository

class LinkGoogleAccountUseCase(
    private val repository: UserProfileRepository,
) {
    suspend operator fun invoke(
        localAccountId: String,
        googleAccount: GoogleAccountProfile,
    ): LinkGoogleAccountResult = repository.linkGoogleAccount(localAccountId, googleAccount)
}
