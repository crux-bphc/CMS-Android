package crux.bphc.cms.models.core

import com.google.gson.annotations.SerializedName

data class AutoLoginDetail(
    @SerializedName("key") val key: String = "",
    @SerializedName("autologinurl") val autoLoginUrl: String = "",
)
