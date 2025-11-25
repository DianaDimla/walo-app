package com.dianadimla.walo.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pod(
    var id: String = "",
    val name: String = "",
    var balance: Double = 0.0,
    var startingBalance: Double = 0.0
) : Parcelable
