package com.project.lighthouse.data.model

import com.google.gson.annotations.SerializedName

data class GmailAuthRequest(
    @SerializedName("authorization_code") val authorizationCode: String? = null,
    @SerializedName("access_token") val accessToken: String? = null,
    @SerializedName("refresh_token") val refreshToken: String? = null
)

data class GmailAuthResponse(
    @SerializedName("authenticated") val authenticated: Boolean,
    @SerializedName("authorization_url") val authorizationUrl: String? = null,
    @SerializedName("message") val message: String
)

data class GmailMessage(
    @SerializedName("id") val id: String,
    @SerializedName("threadId") val threadId: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("from") val from: String,
    @SerializedName("date") val date: String,
    @SerializedName("snippet") val snippet: String,
    @SerializedName("body") val body: String,
    @SerializedName("labels") val labels: List<String> = emptyList()
)

data class GmailMessagesResponse(
    @SerializedName("messages") val messages: List<GmailMessage>,
    @SerializedName("total") val total: Int
)

data class SendEmailRequest(
    @SerializedName("to") val to: String,
    @SerializedName("subject") val subject: String,
    @SerializedName("body") val body: String
)

data class SendEmailResponse(
    @SerializedName("id") val id: String,
    @SerializedName("threadId") val threadId: String,
    @SerializedName("success") val success: Boolean
)

