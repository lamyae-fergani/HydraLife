package com.example.hydralife

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class OnboardingPage(
    val titleRes: Int,
    val descRes: Int,
    val imageRes: Int
)

class OnboardingPagerAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingPagerAdapter.PageViewHolder>() {

    class PageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIllustration: ImageView = itemView.findViewById(R.id.ivIllustration)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val page = pages[position]
        holder.ivIllustration.setImageResource(page.imageRes)
        holder.tvTitle.setText(page.titleRes)
        holder.tvDescription.setText(page.descRes)
    }

    override fun getItemCount(): Int = pages.size
}
