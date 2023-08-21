package com.ahsan.kotlinpractice.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.ahsan.kotlinpractice.Utilities.Utils
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.example.movieappkotlin.R
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class MovieDetailsActivity : AppCompatActivity(), AppBarLayout.OnOffsetChangedListener {

    private lateinit var imageView: ImageView
    private lateinit var  appbar_title: TextView
    private  lateinit var  appbar_subtitle:TextView
    private  lateinit var  date:TextView
    private  lateinit var  time:TextView
    private  lateinit var  title:TextView
    private  lateinit var  description:TextView
    private var isHideToolbarView = false
    private var date_behavior: FrameLayout? = null
    private var titleAppbar: LinearLayout? = null
    private lateinit var appBarLayout: AppBarLayout
    private var toolbar: Toolbar? = null
    private var mDescription: String? = null
    private  var mImg:kotlin.String? = null
    private  var mTitle:kotlin.String? = null
    private  var mDate:kotlin.String? = null
    private  var mSource:kotlin.String? = null
    private  var mGenres:kotlin.String? = null
    private lateinit var collapsingToolbarLayout: CollapsingToolbarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movie_details)

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        supportActionBar?.setTitle("")
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle("");

        appBarLayout = findViewById(R.id.appbar)
        appBarLayout.addOnOffsetChangedListener(this)
        date_behavior = findViewById(R.id.date_behavior)
        titleAppbar = findViewById(R.id.title_appbar)
        imageView = findViewById(R.id.backdrop)
        appbar_title = findViewById(R.id.title_on_appbar)
        appbar_subtitle = findViewById(R.id.subtitle_on_appbar)
        date = findViewById(R.id.date)
        time = findViewById(R.id.time)
        title = findViewById(R.id.title)
        description = findViewById(R.id.tv_description)

        val intent = intent
        mDescription = intent.getStringExtra("description")
        mImg = intent.getStringExtra("img")
        mTitle = intent.getStringExtra("title")
        mDate = intent.getStringExtra("release_date")
        mGenres = intent.getStringExtra("genre_ids")

        val requestOptions = RequestOptions()
        requestOptions.error(Utils.randomDrawbleColor)

        val into = Glide.with(this)
            .load("https://image.tmdb.org/t/p/w500" +mImg)
            .apply(requestOptions)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)

        appbar_title.setText(mTitle)
        appbar_subtitle.setText(mDescription)
        description.setText(mDescription)
        date.setText(Utils.formatDate(mDate))
        title.setText(mTitle)

        val author: String
        author = if (mGenres != null || mGenres !== "") {
            " \u2022 $mGenres"
        } else {
            ""
        }
        val listType: Type = object : TypeToken<ArrayList<String>>() {}.type
        val gson = Gson()
        val genresList: ArrayList<String> = gson.fromJson(mGenres, listType)

        for(genre in genresList){
            //search in genre list
        }
//        Toast.makeText(this, "${arrayList.size}", Toast.LENGTH_SHORT).show()
        time.setText(author + " \u2022 " + Utils.formatDate(mDate))
    }

    override fun onBackPressed() {
        super.onBackPressed()
        supportFinishAfterTransition()
    }

    override fun onOffsetChanged(appBarLayout: AppBarLayout, i: Int) {
        val maxScroll = appBarLayout.totalScrollRange
        val percentage = Math.abs(i).toFloat() / maxScroll.toFloat()
        if (percentage == 1f && isHideToolbarView) {
            date_behavior!!.visibility = View.GONE
            titleAppbar!!.visibility = View.VISIBLE
            isHideToolbarView = !isHideToolbarView
        } else if (percentage < 1f && !isHideToolbarView) {
            date_behavior!!.visibility = View.VISIBLE
            titleAppbar!!.visibility = View.GONE
            isHideToolbarView = !isHideToolbarView
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_news, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.getItemId()
        if (id == R.id.action_share) {
            try {
                val i = Intent(Intent.ACTION_SEND)
                i.type = "text/plan"
                i.putExtra(Intent.EXTRA_SUBJECT, mSource)
                val body = "$mTitle\n$mDescription\nShare from the News App\n"
                i.putExtra(Intent.EXTRA_TEXT, body)
                startActivity(Intent.createChooser(i, "Share with :"))
            } catch (e: Exception) {
                Toast.makeText(this, "Hmm.. Sorry, \nCannot be share", Toast.LENGTH_SHORT).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}