package com.airbnb.lottie.samples

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.airbnb.lottie.FontAssetDelegate
import com.airbnb.lottie.L
import com.airbnb.lottie.LottieComposition
import com.airbnb.lottie.samples.model.CompositionArgs
import com.airbnb.lottie.samples.views.BackgroundColorView
import com.airbnb.lottie.samples.views.ControlBarItemToggleView
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.fragmentViewModel
import com.airbnb.mvrx.withState
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.control_bar.*
import kotlinx.android.synthetic.main.control_bar_background_color.*
import kotlinx.android.synthetic.main.control_bar_mode.*
import kotlinx.android.synthetic.main.control_bar_player_controls.*
import kotlinx.android.synthetic.main.control_bar_scale.*
import kotlinx.android.synthetic.main.control_bar_speed.*
import kotlinx.android.synthetic.main.fragment_player.*
import kotlin.math.min
import kotlin.math.roundToInt

class PlayerFragment : BaseMvRxFragment() {

    private var prefs: SharedPreferences? = null
    private val transition = AutoTransition().apply { duration = 175 }

    private val animatorListener = AnimatorListenerAdapter(
            onStart = { playButton.isActivated = true },
            onEnd = {
                playButton.isActivated = false
            },
            onCancel = {
                playButton.isActivated = false
            },
            onRepeat = {
            }
    )

    private val viewModel: PlayerViewModel by fragmentViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_player, container, false)

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        setHasOptionsMenu(true)

        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        animationView.setFontAssetDelegate(object : FontAssetDelegate() {
            override fun fetchFont(fontFamily: String?): Typeface {
                return Typeface.DEFAULT
            }
        })

        val args = arguments?.getParcelable<CompositionArgs>(EXTRA_ANIMATION_ARGS)
                ?: throw IllegalArgumentException("No composition args specified")
        args.animationData?.bgColorInt?.let {
            backgroundButton1.setBackgroundColor(it)
            animationContainer.setBackgroundColor(it)
            invertColor(it)
        }


        animationView.setBackgroundResource(0)
        animationView.setApplyingOpacityToLayersEnabled(true)

        args.animationDataV2?.bgColorInt?.let {
            backgroundButton1.setBackgroundColor(it)
            animationContainer.setBackgroundColor(it)
            invertColor(it)
        }

        viewModel.fetchAnimation(args)
        viewModel.asyncSubscribe(PlayerState::composition, onFail = {
            Snackbar.make(coordinatorLayout, R.string.composition_load_error, Snackbar.LENGTH_LONG).show()
            Log.w(L.TAG, "Error loading composition.", it)
        }) {
            loadingView.isVisible = false
            onCompositionLoaded(it)
        }

        viewModel.selectSubscribe(PlayerState::lottieURL) {
            if(it != "") {
                prefs?.edit()?.putString(PREF_LOTTIE_URL, it)?.apply()
            }
        }

        viewModel.selectSubscribe(PlayerState::controlsVisible) { controlsContainer.animateVisible(it) }

        viewModel.selectSubscribe(PlayerState::controlBarVisible) { controlBar.animateVisible(it) }

        backgroundColorToggle.setOnClickListener { viewModel.toggleBackgroundColorVisible() }
        closeBackgroundColorButton.setOnClickListener { viewModel.setBackgroundColorVisible(false) }
        viewModel.selectSubscribe(PlayerState::backgroundColorVisible) {
            backgroundColorToggle.isActivated = it
            backgroundColorContainer.animateVisible(it)
        }

        scaleToggle.setOnClickListener { viewModel.toggleScaleVisible() }
        closeScaleButton.setOnClickListener { viewModel.setScaleVisible(false) }
        viewModel.selectSubscribe(PlayerState::scaleVisible) {
            scaleToggle.isActivated = it
            scaleContainer.animateVisible(it)
        }

        speedToggle.setOnClickListener { viewModel.toggleSpeedVisible() }
        closeSpeedButton.setOnClickListener { viewModel.setSpeedVisible(false) }
        viewModel.selectSubscribe(PlayerState::speedVisible) {
            speedToggle.isActivated = it
            speedContainer.isVisible = it
        }
        viewModel.selectSubscribe(PlayerState::speed) {
            animationView.speed = it
            speedButtonsContainer
                    .children
                    .filterIsInstance<ControlBarItemToggleView>()
                    .forEach { toggleView ->
                        toggleView.isActivated = toggleView.getText().replace("x", "").toFloat() == animationView.speed
                    }
        }
        speedButtonsContainer
                .children
                .filterIsInstance(ControlBarItemToggleView::class.java)
                .forEach { child ->
                    child.setOnClickListener {
                        val speed = (it as ControlBarItemToggleView)
                                .getText()
                                .replace("x", "")
                                .toFloat()
                        viewModel.setSpeed(speed)
                    }
                }

        modeToggle.setOnClickListener { viewModel.toggleModeVisible() }
        closeModeButton.setOnClickListener { viewModel.setModeVisible(false) }
        viewModel.selectSubscribe(PlayerState::modeVisible) {
            modeToggle.isActivated = it
            modeContainer.isVisible = it
        }
        viewModel.selectSubscribe(PlayerState::animationMode) {
            modeButtonsContainer
                    .children
                    .filterIsInstance<ControlBarItemToggleView>()
                    .forEach { toggleView ->
                        toggleView.isActivated = toggleView.getText() == it
                    }
            if (it == getString(R.string.infinite)) {
                animationView.repeatCount = ValueAnimator.INFINITE
            } else {
                val mode = it
                animationView.setOnClickListener {
                    if (mode == getString(R.string.on_touch)) {
                        animationView.resumeAnimation()
                    }
                }
                animationView.repeatCount = 1
            }
            animationView.resumeAnimation()
            playButton.isActivated = animationView.isAnimating
        }
        modeButtonsContainer
                .children
                .filterIsInstance(ControlBarItemToggleView::class.java)
                .forEach { child ->
                    child.setOnClickListener {
                        val animationMode = (it as ControlBarItemToggleView)
                                .getText()
                        viewModel.setMode(animationMode)
                    }
                }


        playButton.isActivated = animationView.isAnimating

        seekBar.setOnSeekBarChangeListener(OnSeekBarChangeListenerAdapter(
                onProgressChanged = { _, progress, _ ->
                    if (seekBar.isPressed && progress in 1..4) {
                        seekBar.progress = 0
                        return@OnSeekBarChangeListenerAdapter
                    }
                    if (animationView.isAnimating) return@OnSeekBarChangeListenerAdapter
                    animationView.progress = progress / seekBar.max.toFloat()
                }
        ))

        animationView.addAnimatorUpdateListener {
            if (seekBar.isPressed) return@addAnimatorUpdateListener
            seekBar.progress = ((it.animatedValue as Float) * seekBar.max).roundToInt()
        }
        animationView.addAnimatorListener(animatorListener)
        playButton.setOnClickListener {
            if (animationView.isAnimating) animationView.pauseAnimation() else animationView.resumeAnimation()
            playButton.isActivated = animationView.isAnimating
            postInvalidate()
        }

        scaleSeekBar.setOnSeekBarChangeListener(OnSeekBarChangeListenerAdapter(
                onProgressChanged = { _, progress, _ ->
                    val minScale = minScale()
                    val maxScale = maxScale()
                    val scale = minScale + progress / 100f * (maxScale - minScale)
                    animationView.scale = scale
                    scaleText.text = "%.0f%%".format(scale * 100)
                }
        ))

        arrayOf<BackgroundColorView>(
                backgroundButton1,
                backgroundButton2,
                backgroundButton3,
                backgroundButton4,
                backgroundButton5,
                backgroundButton6
        ).forEach { bb ->
            bb.setOnClickListener {
                animationContainer.setBackgroundColor(bb.getColor())
                invertColor(bb.getColor())
            }
        }

    }

    private fun View.animateVisible(visible: Boolean) {
        beginDelayedTransition()
        isVisible = visible
    }

    private fun invertColor(color: Int) {
        val isDarkBg = color.isDark()
        animationView.isActivated = isDarkBg
        toolbar.isActivated = isDarkBg
    }

    private fun Int.isDark(): Boolean {
        val y = (299 * Color.red(this) + 587 * Color.green(this) + 114 * Color.blue(this)) / 1000
        return y < 128
    }

    override fun onDestroyView() {
        animationView.removeAnimatorListener(animatorListener)
        super.onDestroyView()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.fragment_player, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.isCheckable) item.isChecked = !item.isChecked
        when (item.itemId) {
            android.R.id.home -> requireActivity().finish()
            R.id.info -> Unit
            R.id.visibility -> {
                viewModel.setDistractionFree(item.isChecked)
                val menuIcon = if (item.isChecked) R.drawable.ic_eye_teal else R.drawable.ic_eye_selector
                item.icon = ContextCompat.getDrawable(requireContext(), menuIcon)
            }
            R.id.setWallpaper -> {
                startActivity(Intent(
                        WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER
                ).apply {
                    putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(requireContext(), MinWallpaperService::class.java)
                    )
                })
            }
        }
        return true
    }

    private fun onCompositionLoaded(composition: LottieComposition?) {
        composition ?: return

        animationView.setComposition(composition)

        // Scale up to fill the screen
        scaleSeekBar.progress = 100
    }

    override fun invalidate() {
    }

    private fun minScale() = 0.05f

    private fun maxScale(): Float = withState(viewModel) { state ->
        val screenWidth = resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = resources.displayMetrics.heightPixels.toFloat()
        val bounds = state.composition()?.bounds
        return@withState min(
                screenWidth / (bounds?.width()?.toFloat() ?: screenWidth),
                screenHeight / (bounds?.height()?.toFloat() ?: screenHeight)
        )
    }

    private fun beginDelayedTransition() = TransitionManager.beginDelayedTransition(container, transition)

    companion object {
        const val EXTRA_ANIMATION_ARGS = "animation_args"

        fun forAsset(args: CompositionArgs): Fragment {
            return PlayerFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(EXTRA_ANIMATION_ARGS, args)
                }
            }
        }
    }
}
