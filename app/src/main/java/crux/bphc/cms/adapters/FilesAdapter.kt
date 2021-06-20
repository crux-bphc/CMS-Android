package crux.bphc.cms.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import crux.bphc.cms.databinding.RowDownloadBinding
import crux.bphc.cms.models.Download
import java.io.File

class FilesAdapter : RecyclerView.Adapter<FilesAdapter.ViewHolder>() {

    private lateinit var context: Context
    var data = listOf<Download>()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    lateinit var onRowClickListener: (File) -> Unit
    lateinit var onDeleteClickListener: (File) -> Unit

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        context = recyclerView.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(RowDownloadBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
    ))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val download = data[position]

        with(holder.binding) {
            downloadNameText.text = download.file.nameWithoutExtension
            downloadDetailsText.text = download.description
            downloadIconImage.setImageResource(download.iconResource)

            deleteImage.setOnClickListener { onDeleteClickListener(download.file) }
            downloadLayout.setOnClickListener { onRowClickListener(download.file) }
        }
    }

    override fun getItemCount() = data.size

    inner class ViewHolder(val binding: RowDownloadBinding) : RecyclerView.ViewHolder(binding.root)

}