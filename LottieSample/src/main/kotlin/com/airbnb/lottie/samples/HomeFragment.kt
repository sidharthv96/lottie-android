package com.airbnb.lottie.samples

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.lottie.L
import com.airbnb.mvrx.BaseMvRxFragment
import com.airbnb.mvrx.fragmentViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_home.*
import kotlinx.android.synthetic.main.fragment_home.view.*
import kotlinx.android.synthetic.main.fragment_player.*
import kotlinx.android.synthetic.main.fragment_player.loadingView

class HomeFragment : BaseMvRxFragment()  {


    private val TAG = HomeFragment::class.qualifiedName
    private val viewModel: HomeViewModel by fragmentViewModel()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            inflater.inflate(R.layout.fragment_home, container, false).apply {
                title_marquee.setTitle("Showcase")
            }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.asyncSubscribe(HomeState::data, onFail = {
            Snackbar.make(coordinatorLayout, R.string.composition_load_error, Snackbar.LENGTH_LONG).show()
            Log.w(L.TAG, "Error loading data.", it)
        }) {
            loadingView.isVisible = false
            recyclerView2.apply {
                layoutManager = GridLayoutManager(requireContext(), 2)
//                adapter = MyAnimationRecyclerViewAdapter(it.data)
            }
            it.data.forEach {
                d -> Log.d(TAG, d.previewUrl)
            }
        }
    }

    override fun invalidate() {
    }
}