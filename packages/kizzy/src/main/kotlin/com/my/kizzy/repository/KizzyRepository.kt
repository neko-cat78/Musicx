package com.my.kizzy.repository

import com.my.kizzy.remote.ApiService
import com.my.kizzy.remote.ImageProxyResponse
import io.ktor.client.call.body

class KizzyRepository {
    private val api = ApiService()

    suspend fun getImages(urls: List<String>): ImageProxyResponse? {
        return api.getImage(urls).getOrNull()?.body()
    }
}