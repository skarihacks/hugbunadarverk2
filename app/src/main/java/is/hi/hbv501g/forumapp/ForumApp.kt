package com.hbv501g.forumapp

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
                                onCreatePost = {
                                    navController.navigate(Routes.CREATE_POST)
                                },
                                onCreateCommunity = {
                                    navController.navigate(Routes.CREATE_COMMUNITY)
                                }
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
