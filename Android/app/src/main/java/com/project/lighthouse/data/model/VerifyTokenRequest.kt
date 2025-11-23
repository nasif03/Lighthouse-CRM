package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class VerifyTokenRequest(
    @SerializedName("id_token")
    val idToken: String
)

