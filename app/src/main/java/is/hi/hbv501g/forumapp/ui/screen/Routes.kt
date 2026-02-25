package com.hbv501g.forumapp.ui.screen

import android.net.Uri

object Routes {
    const val FEED = "feed"
    const val COMMUNITIES = "communities"
    const val CREATE_POST = "create-post"
    const val CREATE_COMMUNITY = "create-community"

    const val POST_ID_ARG = "postId"
    const val POST_DETAIL = "post/{$POST_ID_ARG}"
    const val COMMUNITY_NAME_ARG = "communityName"
    const val COMMUNITY_PROFILE = "community/{$COMMUNITY_NAME_ARG}"

    fun postDetail(postId: String): String = "post/$postId"
    fun communityProfile(communityName: String): String = "community/${Uri.encode(communityName)}"
}
