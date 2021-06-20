package crux.bphc.cms.models

import java.io.File

data class Download(
        val file: File,
        val description: String,
        val iconResource: Int
)
