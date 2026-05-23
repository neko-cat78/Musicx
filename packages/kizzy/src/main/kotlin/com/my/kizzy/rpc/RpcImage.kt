package com.my.kizzy.rpc

import com.my.kizzy.repository.KizzyRepository

sealed class RpcImage {

    class DiscordImage(val image: String) : RpcImage() {
    }

    class ExternalImage(val image: String) : RpcImage() {
    }
}