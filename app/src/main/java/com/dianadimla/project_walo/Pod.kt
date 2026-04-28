/**
 * Data model representing a budget pod for expense management.
 */
package com.dianadimla.project_walo

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Pod(
    var id: String = "",       // Unique ID for database reference
    val name: String = "",     // User-defined name for the pod
    var balance: Double = 0.0  // Current monetary balance
) : Parcelable
