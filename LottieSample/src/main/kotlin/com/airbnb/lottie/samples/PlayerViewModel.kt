package com.airbnb.lottie.samples

import android.animation.ValueAnimator
import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.preference.PreferenceManager
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieTask
import com.airbnb.lottie.samples.model.CompositionArgs
import com.airbnb.mvrx.*
import java.io.FileInputStream

data class PlayerState(
        val lottieURL: String = "",
        val composition: Async<LottieComposition> = Uninitialized,
        val controlsVisible: Boolean = true,
        val controlBarVisible: Boolean = true,
        val backgroundColorVisible: Boolean = false,
        val scaleVisible: Boolean = false,
        val speedVisible: Boolean = false,
        val modeVisible: Boolean = false,
        val background: Int = Color.BLACK,
        val scale: Float = 1f,
        val speed: Float = 1f,
        val repeatCount: Int = ValueAnimator.INFINITE,
        val animationMode: String = "Infinite"
) : MvRxState

class PlayerViewModel(
        initialState: PlayerState,
        private val application: Application
) : MvRxViewModel<PlayerState>(initialState) {

    fun fetchAnimation(args: CompositionArgs) {
        val url = args.url ?: args.animationDataV2?.file ?: args.animationData?.lottieLink
        when {
            url != null -> {
                setState {
                    copy(
                            lottieURL = url
                    )
                }
                LottieCompositionFactory.fromUrl(application, url, null)
            }
            args.fileUri != null -> taskForUri(args.fileUri)
            args.asset != null -> LottieCompositionFactory.fromAsset(application, args.asset, null)
            else -> error("Don't know how to fetch animation for $args")
        }
                .addListener {
                    setState {
                        copy(composition = Success(it))
                    }
                }
                .addFailureListener { setState { copy(composition = Fail(it)) } }
    }

    private fun taskForUri(uri: Uri): LottieTask<LottieComposition> {
        val fis = when (uri.scheme) {
            "file" -> FileInputStream(uri.path ?: error("File has no path!"))
            "content" -> application.contentResolver.openInputStream(uri)
            else -> error("Unknown scheme ${uri.scheme}")
        }

        return LottieCompositionFactory.fromJsonInputStream(fis, null)
    }

    fun toggleModeVisible() = setState { copy(modeVisible = !modeVisible) }

    fun setModeVisible(visible: Boolean) = setState { copy(modeVisible = visible) }

    fun setMode(mode: String) = setState { copy(animationMode = mode) }

    fun toggleBackgroundColorVisible() = setState { copy(backgroundColorVisible = !backgroundColorVisible) }

    fun setBackgroundColorVisible(visible: Boolean) = setState { copy(backgroundColorVisible = visible) }

    fun toggleScaleVisible() = setState { copy(scaleVisible = !scaleVisible) }

    fun setScale(scale: Float) = setState { copy(scale = scale) }

    fun setBackground(color: Int) = setState { copy(background = color) }

    fun setScaleVisible(visible: Boolean) = setState { copy(scaleVisible = visible) }

    fun toggleSpeedVisible() = setState { copy(speedVisible = !speedVisible) }

    fun setSpeedVisible(visible: Boolean) = setState { copy(speedVisible = visible) }

    fun setSpeed(speed: Float) = setState { copy(speed = speed) }

    fun setDistractionFree(distractionFree: Boolean) = setState {
        copy(
                controlsVisible = !distractionFree,
                controlBarVisible = !distractionFree,
                modeVisible = false,
                backgroundColorVisible = false,
                scaleVisible = false,
                speedVisible = false
        )
    }

    fun applyWallpaper() {
        withState {
            PreferenceManager.getDefaultSharedPreferences(application).edit()
                    .putString(PrefKeys.LOTTIE_URL, it.lottieURL)
                    .putString(PrefKeys.ANIMATION_MODE, it.animationMode)
                    .putInt(PrefKeys.BACKGROUND, it.background)
                    .putFloat(PrefKeys.SCALE, it.scale)
                    .putFloat(PrefKeys.SPEED, it.speed)
                    .apply()
        }
        val wallpaperInfo = WallpaperManager.getInstance(application).wallpaperInfo

        val componentName = ComponentName(application, MinWallpaperService::class.java)
        if(wallpaperInfo != null && componentName.className==wallpaperInfo.serviceName){
            return
        }
        application.startActivity(Intent(
                WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    componentName
            )
        })
    }

    companion object : MvRxViewModelFactory<PlayerViewModel, PlayerState> {
        override fun create(viewModelContext: ViewModelContext, state: PlayerState): PlayerViewModel? {
            return PlayerViewModel(state, viewModelContext.app())
        }
    }
}