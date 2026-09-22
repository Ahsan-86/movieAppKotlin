package com.ahsan.movieapp.ui.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ahsan.movieapp.R
import com.ahsan.movieapp.data.repository.SessionState

/**
 * Phase 1 ships Guest mode end-to-end. Phase 4 swaps the disabled Log In / Sign Up buttons
 * here for real Firebase Authentication, without touching anything else in the app — every
 * other screen already only cares about [SessionState], not how you got it.
 */
@Composable
fun AccountScreen(viewModel: AccountViewModel = hiltViewModel()) {
    val session by viewModel.sessionState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = stringResource(R.string.nav_account), style = MaterialTheme.typography.displaySmall)

        Text(
            text = when (session) {
                is SessionState.Guest -> stringResource(R.string.account_guest_message)
                is SessionState.SignedIn -> stringResource(R.string.account_signed_in)
                SessionState.SignedOut -> stringResource(R.string.account_signed_out)
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        when (session) {
            is SessionState.SignedIn -> {
                OutlinedButton(onClick = { viewModel.signOut() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.log_out))
                }
            }
            else -> {
                Button(onClick = { viewModel.continueAsGuest() }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.continue_as_guest))
                }
                OutlinedButton(
                    onClick = { /* Wired up to Firebase Authentication in Phase 4 */ },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(stringResource(R.string.account_log_in_coming_soon))
                }
                OutlinedButton(
                    onClick = { /* Wired up to Firebase Authentication in Phase 4 */ },
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    Text(stringResource(R.string.account_sign_up_coming_soon))
                }
            }
        }
    }
}
