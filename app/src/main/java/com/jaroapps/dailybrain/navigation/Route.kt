package com.jaroapps.dailybrain.navigation

object Route {
    const val STARTUP = "startup"
    const val APP_ONBOARDING = "app_onboarding"
    const val BRAIN_ARTICLE = "brain_training_article"
    const val EXERCISE_ENTRY = "exercise_entry/{exercise}"
    const val EXERCISE_INTRO = "exercise_intro/{exercise}"
    const val EXERCISE_RULES = "exercise_rules/{exercise}"

    fun exerciseEntry(exercise: String): String = "exercise_entry/$exercise"
    fun exerciseIntro(exercise: String): String = "exercise_intro/$exercise"
    fun exerciseRules(exercise: String): String = "exercise_rules/$exercise"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val PROFILE_SELECTION = "profile_selection"
    const val PROFILE_SETTINGS = "profile_settings"
    const val PARAMETERS = "profile_parameters"
    const val FRIENDS = "friends"
    const val USER_PROFILE = "user_profile/{userId}"
    const val USER_PROFILE_CONNECTIONS = "user_profile_connections/{userId}/{connectionType}"
    const val PROFILE_COLLECTIBLES = "profile_collectibles/{ownerUid}/{kind}"
    const val COLLECTIBLE_DETAIL = "collectible_detail/{ownerUid}/{kind}/{itemId}"
    const val EDIT_PROFILE = "edit_profile"
    const val AVATAR_EDITOR = "avatar_editor"
    const val STATISTICS = "statistics"
    const val FEED = "feed"
    const val SHOP = "shop"
    const val ARITHMETIC = "arithmetic"
    const val CALENDAR = "calendar"
    const val DAY_HISTORY = "day_history/{date}"
    const val REWARD_HISTORY = "reward_history/{currency}"
    const val MEMORY_TRAINING = "memory_training"
    const val STROOP_TEST = "stroop_test"
    const val COUNTING_TRAINING = "counting_training"

    fun dayHistory(date: Int): String = "day_history/$date"
    fun rewardHistory(currency: String): String = "reward_history/$currency"
    fun userProfile(userId: String): String = "user_profile/$userId"
    fun userProfileConnections(userId: String, connectionType: String): String =
        "user_profile_connections/$userId/$connectionType"
    fun profileCollectibles(ownerUid: String, kind: String): String =
        "profile_collectibles/$ownerUid/$kind"
    fun collectibleDetail(ownerUid: String, kind: String, itemId: String): String =
        "collectible_detail/$ownerUid/$kind/$itemId"
}