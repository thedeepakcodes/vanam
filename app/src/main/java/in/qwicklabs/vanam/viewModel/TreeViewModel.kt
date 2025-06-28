package `in`.qwicklabs.vanam.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.qwicklabs.vanam.model.Tree
import `in`.qwicklabs.vanam.repository.TreeRepository
import kotlinx.coroutines.launch

class TreeViewModel() : ViewModel() {

    private val _trees = MutableLiveData<Tree>()
    val trees: MutableLiveData<Tree> = _trees

    fun addTree(tree: Tree) {
        viewModelScope.launch {
            TreeRepository.addTree(tree.id, tree)
        }
    }
}
