package com.airbnb.lottie.samples

import android.app.Application
import com.airbnb.lottie.samples.model.AnimationResponseV2
import com.airbnb.mvrx.*
import io.reactivex.schedulers.Schedulers

data class HomeState(
        val data: Async<AnimationResponseV2> = Uninitialized
) : MvRxState

class HomeViewModel(
        initialState: HomeState,
        private val application: Application,
        private val api: LottiefilesApi
) : MvRxViewModel<HomeState>(initialState) {

    init {
        api.getCollection()
                .subscribeOn(Schedulers.io())
                .retry(3)
                .execute { copy(data = it) }
    }

    companion object : MvRxViewModelFactory<HomeViewModel, HomeState> {
        override fun create(viewModelContext: ViewModelContext, state: HomeState): HomeViewModel? {
            val service = viewModelContext.app<LottieApplication>().lottiefilesService
            return HomeViewModel(state, viewModelContext.app(), service)
        }
    }
}