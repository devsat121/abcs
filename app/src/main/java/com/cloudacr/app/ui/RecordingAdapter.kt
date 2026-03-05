package com.cloudacr.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cloudacr.app.R
import com.cloudacr.app.data.Recording
import com.cloudacr.app.databinding.ItemRecordingBinding
import java.text.SimpleDateFormat
import java.util.*

class RecordingAdapter(
    private val onItemClick: (Recording) -> Unit,
    private val onItemLongClick: (Recording) -> Boolean,
    private val onStarClick: (Recording) -> Unit
) : ListAdapter<Recording, RecordingAdapter.ViewHolder>(DIFF_CALLBACK) {

    private var selectedIds: Set<Long> = emptySet()

    fun setSelectedIds(ids: Set<Long>) {
        selectedIds = ids
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: ItemRecordingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recording: Recording) {
            val dateFormat = SimpleDateFormat("MMM d, yyyy  h:mm a", Locale.getDefault())
            binding.tvName.text = recording.displayName
            binding.tvDate.text = dateFormat.format(Date(recording.timestamp))
            binding.tvDuration.text = recording.formattedDuration
            binding.tvSize.text = recording.formattedSize
            binding.tvCallType.text = when (recording.callType) {
                Recording.CallType.INCOMING -> "↙ Incoming"
                Recording.CallType.OUTGOING -> "↗ Outgoing"
                Recording.CallType.UNKNOWN -> "↕ Call"
            }
            binding.ivStar.setImageResource(
                if (recording.isStarred) R.drawable.ic_star_filled else R.drawable.ic_star_outline
            )
            val isSelected = recording.id in selectedIds
            binding.root.isActivated = isSelected
            binding.root.setBackgroundResource(
                if (isSelected) R.drawable.bg_item_selected else R.drawable.bg_item_normal
            )
            if (recording.contactName != null && recording.phoneNumber.isNotBlank()) {
                binding.tvPhone.visibility = View.VISIBLE
                binding.tvPhone.text = recording.phoneNumber
            } else {
                binding.tvPhone.visibility = View.GONE
            }
            binding.ivStar.setOnClickListener { onStarClick(recording) }
            binding.root.setOnClickListener { onItemClick(recording) }
            binding.root.setOnLongClickListener { onItemLongClick(recording) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemRecordingBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(getItem(position))

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Recording>() {
            override fun areItemsTheSame(a: Recording, b: Recording) = a.id == b.id
            override fun areContentsTheSame(a: Recording, b: Recording) = a == b
        }
    }
}
