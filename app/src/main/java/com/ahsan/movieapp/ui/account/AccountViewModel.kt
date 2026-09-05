package com.ahsan.movieapp.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahsan.movieapp.data.repository.PreferencesRepository
import com.ahsan.movieapp.data.repository.SessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AccountViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = preferencesRepository.sessionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SessionState.SignedOut)

    fun continueAsGuest() {
        viewModelScope.launch { preferencesRepository.continueAsGuest() }
    }

    fun signOut() {
        viewModelScope.launch { preferencesRepository.signOut() }
    }
}
