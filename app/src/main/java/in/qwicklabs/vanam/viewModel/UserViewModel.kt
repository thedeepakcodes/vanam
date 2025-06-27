package `in`.qwicklabs.vanam.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.ListenerRegistration
import `in`.qwicklabs.vanam.model.User
import `in`.qwicklabs.vanam.repository.UserRepository
import kotlinx.coroutines.launch

class UserViewModel : ViewModel() {
    private val _user = MutableLiveData<User>()
    val user: LiveData<User> get() = _user

    private var userListener: ListenerRegistration? = null

    init {
        listenToUserData()
    }

    private fun listenToUserData() {
        userListener = UserRepository.listenToUserData { updatedUser ->
            updatedUser?.let { _user.postValue(it) }
        }
    }

    fun updateUser(user: User) {
        viewModelScope.launch {
            UserRepository.updateUser(user)
        }
    }

    fun refreshUser() {
        viewModelScope.launch {
            val user = UserRepository.getUser()
            user?.let { _user.value = it }
        }
    }

    override fun onCleared() {
        super.onCleared()
        userListener?.remove()
    }
}
