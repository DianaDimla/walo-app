package com.dianadimla.project_walo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pod(
    var id: String = "",
    val name: String = "",
    var balance: Double = 0.0
) : Parcelable
