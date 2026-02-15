package com.hbv501g.forumapp.ui.screen

object Routes {
    const val FEED = "feed"
    const val CREATE_POST = "create-post"
    const val CREATE_COMMUNITY = "create-community"

    const val POST_ID_ARG = "postId"
    const val POST_DETAIL = "post/{$POST_ID_ARG}"

    fun postDetail(postId: String): String = "post/$postId"
}
