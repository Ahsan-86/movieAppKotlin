package com.ahsan.kotlinpractice.activities

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.core.view.ViewCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ahsan.kotlinpractice.adapters.MovieAdapter
import com.ahsan.kotlinpractice.models.Movie
import com.ahsan.kotlinpractice.models.MovieViewModel
import com.example.movieappkotlin.R
import com.example.movieappkotlin.databinding.ActivityMainBinding
import io.paperdb.Paper


class MainActivity : AppCompatActivity(){

    private lateinit var binding : ActivityMainBinding
    private lateinit var viewModel: MovieViewModel
    private lateinit var movieAdapter : MovieAdapter
    private var isSort = false
    private lateinit var imageViewNoInternet: ImageView
    private lateinit var tvViewNoInternet: TextView
    private lateinit var imageViewShadow: ImageView
    private lateinit var  recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Paper.init(this)

//        val retrofitService = MovieService.getInstance()
//        movieRepository = MovieRepository(retrofitService)

        imageViewNoInternet = findViewById(R.id.imgNoInternet)
        tvViewNoInternet = findViewById(R.id.tvNoInternet)
        imageViewShadow = findViewById(R.id.img)
        recyclerView = findViewById(R.id.rv_movies)
        tvViewNoInternet.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                Log.d("MainActivity", "data "+ Paper.book().read("lastViewData", null))
                if(Paper.book().read("lastViewData", null) != null) {
                    Log.d("MainActivity", "data true")
                    showDetails(Paper.book().read("lastViewData", null) as Movie)
                }else{
                    Log.d("MainActivity", "data false")
                    Toast.makeText(this@MainActivity, "Sorry, no information saved last time", Toast.LENGTH_SHORT).show()
                }
            }
        })

        viewModel = ViewModelProvider(this)[MovieViewModel::class.java]

//        // Observe lastViewedMovie LiveData
//        viewModel.lastViewedMovie.observe(this, Observer { lastMovie ->
//            if (lastMovie != null) {
//                // Update your UI with the last viewed movie information
//                lastViewData = lastMovie
//                isAvailableLastViewData = true
//                tvViewNoInternet.setText("View last viewed movie information")
//            } else {
//                isAvailableLastViewData = false
//                tvViewNoInternet.setText("No last viewed movie information found")
//            }
//        })

        if(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                !isOnline(this)
            } else {
                TODO("VERSION.SDK_INT < M")
            }
        ){
            //no internet
            updateUI(false)
        }else{
            updateUI(true)
        }

        prepareRecyclerView()
        movieAdapter.onItemClick = { item ->
            Log.d("TAG", item.title)
            showDetails(item)
        }

//        val retrofitService = MovieService.getInstance()
//        val mainRepository = MovieRepository(retrofitService)
//        binding.rvMovies.adapter = movieAdapter

//        viewModel = ViewModelProvider(this, MovieViewModelFactory(mainRepository)).get(MovieViewModel::class.java)
//        viewModel.observeMovieLiveData().observe(this, {
//            movieAdapter.setMovieList(it)
//        })
//
//        viewModel.errorMessage.observe(this, {
//            Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
//        })
//
//        viewModel.loading.observe(this, Observer {
//            if (it) {
//                binding.progressDialog.visibility = View.VISIBLE
//            } else {
//                binding.progressDialog.visibility = View.GONE
//            }
//        })
//
//        viewModel.getAllUsers()

//        userViewModel.getUsers().observe(this,
//            Observer {
//                when(it){
//                    is UserRepository.ResponseResult.Success<*> -> {
//                        val users = it.data
//                        // notify recyclerView
//                    }
//                    is UserRepository.ResponseResult.Failure -> {
//                        val error = it.error
//                        // some error happened
//                    }
//                    is UserRepository.ResponseResult.Pending-> {
//                        // start | in progress
//                        // show Loading...
//                    }
//                    is UserRepository.ResponseResult.Complete-> {
//                        // call ended | completed
//                        // hide Loading...
//                    }
//                }
//            });
    }

    fun showDetails(item: Movie){
        val imageView: ImageView = findViewById(R.id.img)
        val intent = Intent(this@MainActivity, MovieDetailsActivity::class.java)

        intent.putExtra("title", item.title)
        intent.putExtra("img", item.poster_path)
        intent.putExtra("release_date", item.release_date)
//            intent.putExtra("source",  article.getSource().getName());
//            intent.putExtra("author",  article.getAuthor());
        intent.putExtra("description", item.overview)
        intent.putExtra("genre_ids", item.genre_ids.toString())

        Paper.book().write("lastViewData", item);

        val pair: Pair<View, String> =
            Pair.create(imageView as View, ViewCompat.getTransitionName(imageView))
        val optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this@MainActivity,
            pair
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            startActivity(intent, optionsCompat.toBundle())
        } else {
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        Paper.book().destroy();
    }

    fun updateUI(flag: Boolean){
        if(flag){
            imageViewShadow.isVisible = true
            recyclerView.isVisible = true
            imageViewNoInternet.isVisible = false
            tvViewNoInternet.isVisible = false
            viewModel.getPopularMovies()
            viewModel.observeMovieLiveData().observe(this, Observer { movieList ->
                movieAdapter.setMovieList(movieList)
            })
        }else{
            imageViewShadow.isVisible = false
            recyclerView.isVisible = false
            imageViewNoInternet.isVisible = true
            tvViewNoInternet.isVisible = true
        }
    }

//    // Function to update last viewed movie
//    private suspend fun updateLastViewedMovie() {
//        val movie = LastViewedMovie(id = 1, title = "Last Movie", description = "Description", date = "2023-04-16", genre = "20,14", image = "test")
//        viewModel.insertOrUpdateLastViewedMovie(movie)
//    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (connectivityManager != null) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
                    return true
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
                    return true
                }
            }
        }
        return false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_main, menu)
        val menuItem = menu?.findItem(R.id.action_search)

        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView: SearchView? = menu.findItem(R.id.action_search).actionView as SearchView?
        val searchMenuItem = menu.findItem(R.id.action_search)
        searchView?.setSearchableInfo(searchManager.getSearchableInfo(componentName))
        searchView?.setQueryHint("Filter Movies By Name...")
        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    movieAdapter.getFilter().filter(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter your RecyclerView data based on the newText
                if (newText != null) {
                    movieAdapter.getFilter().filter(newText)
                }
                return false
            }
        })
        searchMenuItem.icon!!.setVisible(false, false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort -> {
                if(this.isSort) {
                    this.isSort = false
                    movieAdapter.sortMovieListAscending()
                }
                else {
                    this.isSort = true
                    movieAdapter.sortMovieListDescending()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun prepareRecyclerView() {
        movieAdapter = MovieAdapter()
        binding.rvMovies.apply {
            layoutManager = GridLayoutManager(applicationContext,2)
            adapter = movieAdapter
        }
    }

    fun initDb(): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "user-db"
        ).build()
    }

}
