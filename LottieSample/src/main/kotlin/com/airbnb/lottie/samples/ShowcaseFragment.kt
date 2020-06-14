package com.airbnb.lottie.samples

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.samples.grid.DummyContent
import com.airbnb.lottie.samples.grid.MyLottieItemRecyclerViewAdapter
import com.airbnb.lottie.samples.model.AnimationResponseV2
import com.airbnb.mvrx.*
import io.reactivex.schedulers.Schedulers

data class ShowcaseState(val response: Async<AnimationResponseV2> = Uninitialized) : MvRxState

class ShowcaseViewModel(initialState: ShowcaseState, api: LottiefilesApi) : MvRxViewModel<ShowcaseState>(initialState) {
    init {
        api.getCollection()
                .subscribeOn(Schedulers.io())
                .retry(3)
                .execute { copy(response = it) }
    }

    companion object : MvRxViewModelFactory<ShowcaseViewModel, ShowcaseState> {
        override fun create(viewModelContext: ViewModelContext, state: ShowcaseState): ShowcaseViewModel? {
            val service = viewModelContext.app<LottieApplication>().lottiefilesService
            return ShowcaseViewModel(state, service)
        }
    }
}

class ShowcaseFragment : Fragment() {

    private var columnCount = 3


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = 3
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_lottie_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            with(view) {
                layoutManager = when {
                    columnCount <= 1 -> LinearLayoutManager(context)
                    else -> GridLayoutManager(context, columnCount)
                }
                adapter = MyLottieItemRecyclerViewAdapter(DummyContent.ITEMS)
            }
        }
        return view
    }
}