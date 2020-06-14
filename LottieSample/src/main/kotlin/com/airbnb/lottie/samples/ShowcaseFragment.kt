package com.airbnb.lottie.samples

import com.airbnb.epoxy.EpoxyController
import com.airbnb.lottie.samples.model.AnimationResponseV2
import com.airbnb.lottie.samples.model.CompositionArgs
import com.airbnb.lottie.samples.views.animationItemView
import com.airbnb.lottie.samples.views.loadingView
import com.airbnb.lottie.samples.views.marquee
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

class ShowcaseFragment : BaseEpoxyFragment() {

    private val viewModel: ShowcaseViewModel by fragmentViewModel()

    override fun EpoxyController.buildModels() = withState(viewModel) { state ->
        marquee {
            id("showcase")
            title("Showcase")
        }

        val collectionItems = state.response()?.data

        if (collectionItems == null) {
            loadingView {
                id("loading")
            }
        } else {
            collectionItems.forEach {
                animationItemView {
                    id(it.id)
                    title(it.title)
                    previewUrl("https://assets9.lottiefiles.com/${it.preview}")
                    previewBackgroundColor(it.bgColorInt)
                    onClickListener { _ -> startActivity(PlayerActivity.intent(requireContext(), CompositionArgs(animationDataV2 = it))) }
                }
            }
        }
    }
}