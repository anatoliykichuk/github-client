package com.geekbrains.githubclient.domain.user

import com.geekbrains.githubclient.data.GithubRepository
import com.geekbrains.githubclient.data.GithubUser
import com.geekbrains.githubclient.data.IGithubRepositoriesRepo
import com.geekbrains.githubclient.di.scope.scopecontainer.IRepositoryScopeContainer
import com.geekbrains.githubclient.domain.repository.IRepositoryItemView
import com.geekbrains.githubclient.domain.repository.IRepositoryListPresenter
import com.geekbrains.githubclient.ui.IScreens
import com.github.terrakok.cicerone.Router
import io.reactivex.rxjava3.core.Scheduler
import moxy.MvpPresenter
import javax.inject.Inject

class UserPresenter(val user: GithubUser) : MvpPresenter<UserView>() {

    @Inject lateinit var uiScheduler: Scheduler
    @Inject lateinit var repositoriesRepo: IGithubRepositoriesRepo
    @Inject lateinit var router: Router
    @Inject lateinit var screens: IScreens
    @Inject lateinit var repositoryScopeContainer: IRepositoryScopeContainer

    class RepositoryListPresenter : IRepositoryListPresenter {
        val repositories = mutableListOf<GithubRepository>()
        override var itemClickListener: ((IRepositoryItemView) -> Unit)? = null

        override fun bindView(view: IRepositoryItemView) {
            val repository = repositories[view.itemPosition]

            repository.name?.let { view.setName(it) }
        }

        override fun getCount() = repositories.size
    }

    val repositoryListPresenter = RepositoryListPresenter()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()

        viewState.init()
        loadData()

        repositoryListPresenter.itemClickListener = {itemView ->
            val repository = repositoryListPresenter.repositories[itemView.itemPosition]
            router.navigateTo(screens.repository(repository))
        }
    }

    override fun onDestroy() {
        repositoryScopeContainer.releaseRepositoryScope()
        super.onDestroy()
    }

    fun loadData() {
        repositoriesRepo.getRepositories(user)
            .observeOn(uiScheduler)
            .subscribe({ githubRepositories ->
                repositoryListPresenter.repositories.apply {
                    clear()
                    addAll(githubRepositories)
                }
                viewState.updateList()
            }, {
                println("Error: ${it.message}")
            })
    }

    fun backPressed(): Boolean {
        router.exit()
        return true
    }
}