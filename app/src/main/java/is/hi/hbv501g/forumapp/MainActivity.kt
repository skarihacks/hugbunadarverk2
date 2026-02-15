package com.hbv501g.forumapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.hbv501g.forumapp.data.network.ApiClient
import com.hbv501g.forumapp.data.repository.ForumRepository
import com.hbv501g.forumapp.data.session.SessionStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sessionStore = SessionStore(applicationContext)
        val repository = ForumRepository(ApiClient.createService(), sessionStore)

        setContent {
            ForumApp(repository = repository)
        }
    }
}
