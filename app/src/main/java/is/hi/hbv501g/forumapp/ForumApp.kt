package com.hbv501g.forumapp

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.ui.screen.CreatePostRoute
import com.hbv501g.forumapp.ui.screen.CommunitiesRoute
import com.hbv501g.forumapp.ui.screen.CommunityProfileRoute
import com.hbv501g.forumapp.ui.screen.CreateCommunityRoute
import com.hbv501g.forumapp.ui.screen.FeedRoute
import com.hbv501g.forumapp.ui.screen.LoginRoute
import com.hbv501g.forumapp.ui.screen.PostDetailRoute
import com.hbv501g.forumapp.ui.screen.Routes
import com.hbv501g.forumapp.ui.screen.SignUpRoute
import com.hbv501g.forumapp.ui.theme.ForumTheme

@Composable
fun ForumApp(repository: ForumRepository) {
    ForumTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val session = repository.sessionFlow.collectAsStateWithLifecycle(initialValue = null).value
            if (session == null) {
                var showingSignUp by rememberSaveable { mutableStateOf(false) }
                if (showingSignUp) {
                    SignUpRoute(
                        repository = repository,
                        onBackToLogin = { showingSignUp = false }
                    )
                } else {
                    LoginRoute(
                        repository = repository,
                        onOpenSignUp = { showingSignUp = true }
                    )
                }
            } else {
                key(session.sessionId) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Routes.FEED
                    ) {
                        composable(Routes.FEED) {
                            FeedRoute(
                                repository = repository,
                                onOpenPost = { postId ->
                                    navController.navigate(Routes.postDetail(postId))
                                },
                                onOpenCommunities = {
                                    navController.navigate(Routes.COMMUNITIES)
                                },
                                onCreatePost = {
                                    navController.navigate(Routes.CREATE_POST)
                                },
                                onCreateCommunity = {
                                    navController.navigate(Routes.CREATE_COMMUNITY)
                                }
                            )
                        }

                        composable(Routes.COMMUNITIES) {
                            CommunitiesRoute(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onOpenCommunity = { name ->
                                    navController.navigate(Routes.communityProfile(name))
                                }
                            )
                        }

                        composable(
                            route = Routes.COMMUNITY_PROFILE,
                            arguments = listOf(navArgument(Routes.COMMUNITY_NAME_ARG) {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val encodedName = backStackEntry.arguments
                                ?.getString(Routes.COMMUNITY_NAME_ARG)
                                .orEmpty()
                            val communityName = Uri.decode(encodedName)
                            CommunityProfileRoute(
                                communityName = communityName,
                                repository = repository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.CREATE_COMMUNITY) {
                            CreateCommunityRoute(
                                repository = repository,
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable(Routes.CREATE_POST) {
                            CreatePostRoute(
                                repository = repository,
                                onBack = { navController.popBackStack() },
                                onPostCreated = { postId ->
                                    navController.navigate(Routes.postDetail(postId)) {
                                        popUpTo(Routes.FEED)
                                    }
                                }
                            )
                        }

                        composable(
                            route = Routes.POST_DETAIL,
                            arguments = listOf(navArgument(Routes.POST_ID_ARG) {
                                type = NavType.StringType
                            })
                        ) { backStackEntry ->
                            val postId = backStackEntry.arguments?.getString(Routes.POST_ID_ARG).orEmpty()
                            PostDetailRoute(
                                postId = postId,
                                repository = repository,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
