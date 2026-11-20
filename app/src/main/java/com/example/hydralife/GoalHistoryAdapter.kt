package com.example.hydralife

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hydralife.data.HydrationGoalEntity

class GoalHistoryAdapter(private val historyList: List<HydrationGoalEntity>) :
    RecyclerView.Adapter<GoalHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvHistoryDate: TextView = itemView.findViewById(R.id.tvHistoryDate)
        val tvHistoryGoal: TextView = itemView.findViewById(R.id.tvHistoryGoal)
        val tvHistoryStatus: TextView = itemView.findViewById(R.id.tvHistoryStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_goal_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val goal = historyList[position]
        val context = holder.itemView.context
        holder.tvHistoryDate.text = goal.dateString
        holder.tvHistoryGoal.text = context.getString(R.string.goal_prefix, goal.goalMl)
        if (goal.completed) {
            holder.tvHistoryStatus.text = context.getString(R.string.completed_status)
            holder.tvHistoryStatus.setTextColor(context.getColor(R.color.primary))
        } else {
            holder.tvHistoryStatus.text = context.getString(R.string.in_progress_status)
            holder.tvHistoryStatus.setTextColor(context.getColor(R.color.on_surface))
        }
    }

    override fun getItemCount(): Int = historyList.size
}
