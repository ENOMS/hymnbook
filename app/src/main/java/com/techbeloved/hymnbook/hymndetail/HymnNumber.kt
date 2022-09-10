package com.techbeloved.hymnbook.hymndetail

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HymnNumber(val index: Int, val preferSheetMusic: Boolean): Parcelable