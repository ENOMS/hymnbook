package com.techbeloved.hymnbook.hymndetail

import androidx.recyclerview.widget.DiffUtil

class HmnPagerDiffer(private val old: List<HymnNumber>, private val new: List<HymnNumber>) :
    DiffUtil.Callback() {
    companion object {
        private const val PAYLOAD_CHANGED = -1
    }

    override fun getOldListSize(): Int = old.size

    override fun getNewListSize(): Int = new.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old[oldItemPosition].index == new[newItemPosition].index
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return old[oldItemPosition] == new[newItemPosition]
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any {
        return listOf(PAYLOAD_CHANGED)
    }
}