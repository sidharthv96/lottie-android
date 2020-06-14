package com.airbnb.lottie.samples

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.multidex.MultiDexApplication
import com.airbnb.lottie.L
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.ncornette.cache.OkCacheControl
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


class LottieApplication : MultiDexApplication(), OkCacheControl.NetworkMonitor {

    private val cacheSize: Long = (25 * 1024 * 1024).toLong()
    private val myCache: Cache by lazy {
        Cache(this.cacheDir, cacheSize)
    }

     override fun isOnline(): Boolean {
        val connectivityManager = this.getSystemService(Context.CONNECTIVITY_SERVICE) as
                ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val nw      = connectivityManager.activeNetwork ?: return false
            val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
            return when {
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
                else -> false
            }
        } else {
            val nwInfo = connectivityManager.activeNetworkInfo ?: return false
            return nwInfo.isConnected
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkCacheControl.on(OkHttpClient.Builder())
                .overrideServerCachePolicy(30, TimeUnit.MINUTES)
                .forceCacheWhenOffline(this)
                .apply()
                .cache(myCache)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build()
    }

    private val gson: Gson by lazy {
        GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
                .baseUrl("https://api.lottiefiles.com/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(okHttpClient)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
    }

    val lottiefilesService: LottiefilesApi by lazy { retrofit.create(LottiefilesApi::class.java) }

    override fun onCreate() {
        super.onCreate()
        L.DBG = true
        @Suppress("RestrictedApi")
        L.setTraceEnabled(true)
    }
}