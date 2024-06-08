package com.lagradost.cloudstream3.ui.download

import android.annotation.SuppressLint
import android.text.format.Formatter.formatShortFileSize
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.DownloadHeaderEpisodeBinding
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.ui.result.DiffCallback
import com.lagradost.cloudstream3.utils.UIHelper.setImage
import com.lagradost.cloudstream3.utils.VideoDownloadHelper

data class VisualDownloadHeaderCached(
    val currentOngoingDownloads: Int,
    val totalDownloads: Int,
    val totalBytes: Long,
    val currentBytes: Long,
    val data: VideoDownloadHelper.DownloadHeaderCached,
    val child: VideoDownloadHelper.DownloadEpisodeCached?,
)

data class DownloadHeaderClickEvent(
    val action: Int,
    val data: VideoDownloadHelper.DownloadHeaderCached
)

class DownloadHeaderAdapter(
    private var cardList: MutableList<VisualDownloadHeaderCached>,
    private val clickCallback: (DownloadHeaderClickEvent) -> Unit,
    private val movieClickCallback: (DownloadClickEvent) -> Unit,
) : ListAdapter<VisualDownloadHeaderCached, DownloadHeaderAdapter.DownloadHeaderViewHolder>(
    DiffCallback
) {

    private var currentList = emptyList<VisualDownloadHeaderCached>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadHeaderViewHolder {
        return DownloadHeaderViewHolder(
            DownloadHeaderEpisodeBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            clickCallback,
            movieClickCallback
        )
    }

    override fun onBindViewHolder(holder: DownloadHeaderViewHolder, position: Int) {
        holder.bind(cardList[position])
    }

    fun submitCustomList(list: List<VisualDownloadHeaderCached>) {
        currentList = list
        submitNewList(list)
    }

    override fun getItemCount(): Int {
        return cardList.count()
    }

    class DownloadHeaderViewHolder (
        val binding: DownloadHeaderEpisodeBinding,
        private val clickCallback: (DownloadHeaderClickEvent) -> Unit,
        private val movieClickCallback: (DownloadClickEvent) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        /*private val poster: ImageView? = itemView.download_header_poster
        private val title: TextView = itemView.download_header_title
        private val extraInfo: TextView = itemView.download_header_info
        private val holder: CardView = itemView.episode_holder

        private val downloadBar: ContentLoadingProgressBar = itemView.download_header_progress_downloaded
        private val downloadImage: ImageView = itemView.download_header_episode_download
        private val normalImage: ImageView = itemView.download_header_goto_child*/

        @SuppressLint("SetTextI18n")
        fun bind(card: VisualDownloadHeaderCached) {
            val d = card.data

            binding.downloadHeaderPoster.apply {
                setImage(d.poster)
                setOnClickListener {
                    clickCallback.invoke(DownloadHeaderClickEvent(1, d))
                }
            }

            binding.apply {

                binding.downloadHeaderTitle.text = d.name
                val mbString = formatShortFileSize(itemView.context, card.totalBytes)

                //val isMovie = d.type.isMovieType()
                if (card.child != null) {
                    //downloadHeaderProgressDownloaded.visibility = View.VISIBLE

                   // downloadHeaderEpisodeDownload.visibility = View.VISIBLE
                    binding.downloadHeaderGotoChild.visibility = View.GONE

                    downloadButton.setDefaultClickListener(card.child, downloadHeaderInfo, movieClickCallback)
                    downloadButton.isVisible = true
                    /*setUpButton(
                        card.currentBytes,
                        card.totalBytes,
                        downloadBar,
                        downloadImage,
                        extraInfo,
                        card.child,
                        movieClickCallback
                    )*/

                    episodeHolder.setOnClickListener {
                        movieClickCallback.invoke(
                            DownloadClickEvent(
                                DOWNLOAD_ACTION_PLAY_FILE,
                                card.child
                            )
                        )
                    }
                } else {
                    downloadButton.isVisible = false
                   // downloadHeaderProgressDownloaded.visibility = View.GONE
                   // downloadHeaderEpisodeDownload.visibility = View.GONE
                    binding.downloadHeaderGotoChild.visibility = View.VISIBLE

                    try {
                        downloadHeaderInfo.text =
                            downloadHeaderInfo.context.getString(R.string.extra_info_format).format(
                                card.totalDownloads,
                                if (card.totalDownloads == 1) downloadHeaderInfo.context.getString(R.string.episode) else downloadHeaderInfo.context.getString(
                                    R.string.episodes
                                ),
                                mbString
                            )
                    } catch (t: Throwable) {
                        // you probably formatted incorrectly
                        downloadHeaderInfo.text = "Error"
                        logError(t)
                    }

                    episodeHolder.setOnClickListener {
                        clickCallback.invoke(DownloadHeaderClickEvent(0, d))
                    }
                }
            }
        }
    }

    private fun submitNewList(newList: List<VisualDownloadHeaderCached>) {
        val diffCallback = DiffCallback(cardList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        cardList.clear()
        cardList.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<VisualDownloadHeaderCached>() {
        override fun areItemsTheSame(oldItem: VisualDownloadHeaderCached, newItem: VisualDownloadHeaderCached): Boolean {
            return oldItem.data.id == newItem.data.id
        }

        override fun areContentsTheSame(oldItem: VisualDownloadHeaderCached, newItem: VisualDownloadHeaderCached): Boolean {
            return oldItem == newItem
        }
    }
}