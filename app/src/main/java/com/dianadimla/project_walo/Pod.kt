package com.dianadimla.project_walo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

// Data class for a single budget pod.
// @param id Unique pod ID.
// @param name User-defined pod name.
// @param balance Current pod balance.

@Parcelize
data class Pod(
    var id: String = "",
    val name: String = "",
    var balance: Double = 0.0
) : Parcelable
