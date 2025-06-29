package `in`.qwicklabs.vanam.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import `in`.qwicklabs.vanam.model.Tree
import `in`.qwicklabs.vanam.repository.TreeRepository
import kotlinx.coroutines.launch

class TreeViewModel : ViewModel() {

    private val _trees = MutableLiveData<List<Tree>>()
    val trees: MutableLiveData<List<Tree>> = _trees

    fun addTree(tree: Tree) {
        viewModelScope.launch {
            TreeRepository.addTree(tree.id, tree)
        }
    }

    fun getMyTrees() {
        viewModelScope.launch {
            val result = TreeRepository.getMyTrees()
            _trees.postValue(result)
        }
    }

    private val _selectedTree = MutableLiveData<Tree?>()
    val selectedTree: MutableLiveData<Tree?> = _selectedTree

    fun getTreeById(treeId: String) {
        viewModelScope.launch {
            val tree = TreeRepository.getTreeById(treeId)
            _selectedTree.postValue(tree)
        }
    }
}
