package com.ahsan.kotlinpractice.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import androidx.recyclerview.widget.RecyclerView
import com.ahsan.kotlinpractice.Utilities.Utils
import com.ahsan.kotlinpractice.databinding.MovieLayoutBinding
import com.ahsan.kotlinpractice.models.Movie
import com.bumptech.glide.Glide
import java.util.*

class MovieAdapter : RecyclerView.Adapter<MovieAdapter.ViewHolder>() {
    private var movieList = ArrayList<Movie>()
    var onItemClick: ((Movie) -> Unit)? = null
    var filteredItemList: ArrayList<Movie> = movieList
    private var allMoviesList = ArrayList<Movie>()

    fun setMovieList(movieList: List<Movie>) {
        this.movieList = movieList as ArrayList<Movie>
        this.allMoviesList = ArrayList(movieList)
        notifyDataSetChanged()
    }

    fun getMovieList(movieList: List<Movie>): ArrayList<Movie> {
        return this.movieList
    }

    inner class ViewHolder(val binding: MovieLayoutBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            itemView.setOnClickListener {
                onItemClick?.invoke(movieList[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            MovieLayoutBinding.inflate(
                LayoutInflater.from(
                    parent.context
                )
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView)
            .load("https://image.tmdb.org/t/p/w500" + movieList[position].poster_path)
            .into(holder.binding.movieImage)
        holder.binding.movieName.text = movieList[position].title
        holder.binding.movieDescription.text = Utils.formatDate(movieList[position].release_date)
    }

    override fun getItemCount(): Int {
        return movieList.size
    }

    fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val filterResults = FilterResults()
                Log.d("Query value", constraint as String)

                if (constraint.isNullOrEmpty()) {
                    Log.d("Query empty", "Query empty")
                    filterResults.values = allMoviesList
                    Log.d("Filter List Size Empty", filterResults.count.toString())
                } else {
                    val filteredList = allMoviesList.filter { movie ->
                        movie.title.contains(constraint, ignoreCase = true) ||
                                movie.overview.contains(constraint, ignoreCase = true)
                    }
                    filterResults.values = filteredList
                    Log.d("Filter List Size Match", filterResults.count.toString())
                }
                return filterResults
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
//                filteredItemList.clear() // Clear the existing list
//                filteredItemList.addAll(results?.values as? List<Movie> ?: emptyList())
//                notifyDataSetChanged()

                Log.d("Filter List Size Before", movieList.size.toString())
                movieList.clear()
                movieList.addAll(results?.values as Collection<Movie>)
                Log.d("Filter List Size After", movieList.size.toString())
                notifyDataSetChanged()
            }
        }
    }

    public fun sortMovieListAscending() {
        movieList.sortBy { it.title }
        notifyDataSetChanged()
    }

    public fun sortMovieListDescending() {
        movieList.sortByDescending { it.title }
        notifyDataSetChanged()
    }
}