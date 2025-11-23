package com.project.lighthouse

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.identity.Identity
import com.project.lighthouse.authentication.GoogleAuthUiClient
import com.project.lighthouse.authentication.SignInScreen
import com.project.lighthouse.authentication.SignInViewModel
import com.project.lighthouse.di.AppModule
import com.project.lighthouse.ui.accounts.AccountsScreen
import com.project.lighthouse.ui.accounts.AccountsViewModel
import com.project.lighthouse.ui.accounts.AccountsViewModelFactory
import com.project.lighthouse.ui.auth.AuthViewModel
import com.project.lighthouse.ui.auth.AuthViewModelFactory
import com.project.lighthouse.ui.common.PlaceholderScreen
import com.project.lighthouse.ui.contacts.ContactsScreen
import com.project.lighthouse.ui.contacts.ContactsViewModel
import com.project.lighthouse.ui.contacts.ContactsViewModelFactory
import com.project.lighthouse.ui.dashboard.DashboardScreen
import com.project.lighthouse.ui.dashboard.DashboardViewModel
import com.project.lighthouse.ui.dashboard.DashboardViewModelFactory
import com.project.lighthouse.ui.deals.DealsScreen
import com.project.lighthouse.ui.deals.DealsViewModel
import com.project.lighthouse.ui.deals.DealsViewModelFactory
import com.project.lighthouse.ui.leads.LeadsScreen
import com.project.lighthouse.ui.leads.LeadsViewModel
import com.project.lighthouse.ui.leads.LeadsViewModelFactory
import com.project.lighthouse.ui.navigation.MainDestination
import com.project.lighthouse.ui.settings.SettingsScreen
import com.project.lighthouse.ui.settings.SettingsViewModel
import com.project.lighthouse.ui.settings.SettingsViewModelFactory
import com.project.lighthouse.ui.theme.LighthouseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var authViewModel: AuthViewModel

    private val googleAuthUiClient by lazy {
        GoogleAuthUiClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize DI module
        AppModule.initialize(applicationContext)

        authViewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(
                AppModule.getAuthRepository(applicationContext),
                AppModule.getTokenManager(applicationContext)
            )
        )[AuthViewModel::class.java]

        setContent {
            LighthouseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    val authState by authViewModel.authState.collectAsStateWithLifecycle()
                    Log.d(TAG, "AuthState updated: isAuthenticated=${authState.isAuthenticated}")

                    // Determine start destination based on auth state
                    val startDestination = if (authState.isAuthenticated) "dashboard" else "sign_in"

                    Scaffold(
                        bottomBar = {
                            val showBottomNav = shouldShowBottomBar(currentDestination)
                            if (showBottomNav) {
                                LighthouseBottomBar(
                                    currentDestination = currentDestination,
                                    onNavigate = { destination ->
                                        navController.navigate(destination.route) {
                                            popUpTo(MainDestination.Dashboard.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                )
                            }
                        }
                    ) { innerPadding ->
                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            composable("sign_in") {
                            // Create SignInViewModel with AuthViewModel using viewModel factory
                            val signInViewModel = viewModel<SignInViewModel>(
                                factory = object : ViewModelProvider.Factory {
                                    @Suppress("UNCHECKED_CAST")
                                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                        return SignInViewModel(authViewModel) as T
                                    }
                                }
                            )
                            val state by signInViewModel.state.collectAsStateWithLifecycle()

                            val launcher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.StartIntentSenderForResult(),
                                onResult = { result ->
                                    if (result.resultCode == Activity.RESULT_OK) {
                                        lifecycleScope.launch {
                                            val signInResult = googleAuthUiClient.signInWithIntent(
                                                intent = result.data ?: return@launch
                                            )
                                            signInViewModel.onSignInResult(signInResult)
                                        }
                                    }
                                }
                            )

                            // Navigate to dashboard when sign-in is successful
                            LaunchedEffect(key1 = state.isSignInSuccessful) {
                                if (state.isSignInSuccessful) {
                                    Log.d(TAG, "Sign-in success, navigating to dashboard")
                                    Toast.makeText(
                                        applicationContext,
                                        "Sign in successful",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    navController.navigate("dashboard") {
                                        popUpTo("sign_in") { inclusive = true }
                                    }
                                    signInViewModel.resetState()
                                }
                            }

                            // Show error if sign-in fails
                            LaunchedEffect(key1 = state.signInError) {
                                state.signInError?.let { error ->
                                    if (error.isNotBlank()) {
                                        Toast.makeText(
                                            applicationContext,
                                            error,
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }

                                SignInScreen(
                                    state = state,
                                    onSignInClick = {
                                        lifecycleScope.launch {
                                            val signInIntentSender = googleAuthUiClient.signIn()
                                            if (signInIntentSender == null) {
                                                Toast.makeText(
                                                    applicationContext,
                                                    "Unable to start Google Sign-In. Check your network connection and try again.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                return@launch
                                            }
                                            launcher.launch(
                                                IntentSenderRequest.Builder(
                                                    signInIntentSender
                                                ).build()
                                            )
                                        }
                                    }
                                )
                            }

                            composable(MainDestination.Dashboard.route) {
                                val dashboardViewModel = viewModel<DashboardViewModel>(
                                    factory = DashboardViewModelFactory(
                                        AppModule.getDashboardRepository(applicationContext)
                                    )
                                )
                                val dashboardState by dashboardViewModel.state.collectAsStateWithLifecycle()

                                DashboardScreen(
                                    state = dashboardState,
                                    userName = authState.user?.name ?: "User",
                                    onRefresh = { dashboardViewModel.refreshDashboard() }
                                )
                            }

                            composable(MainDestination.Leads.route) {
                                val leadsViewModel = viewModel<LeadsViewModel>(
                                    factory = LeadsViewModelFactory(AppModule.getLeadsRepository(applicationContext))
                                )
                                val leadsState by leadsViewModel.state.collectAsStateWithLifecycle()

                                LeadsScreen(
                                    state = leadsState,
                                    onRefresh = { leadsViewModel.refreshLeads() },
                                    onCreateLead = { leadsViewModel.createLead() },
                                    onUpdateForm = { name, email, phone, source ->
                                        leadsViewModel.updateForm(name, email, phone, source)
                                    },
                                    onToggleDialog = { leadsViewModel.toggleCreateDialog(it) },
                                    onUpdateStatus = { id, status -> leadsViewModel.updateLeadStatus(id, status) },
                                    onConvertLead = { leadsViewModel.convertLead(it) },
                                    onDeleteLead = { leadsViewModel.deleteLead(it) },
                                    onDismissMessage = { leadsViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.Contacts.route) {
                                val contactsViewModel = viewModel<ContactsViewModel>(
                                    factory = ContactsViewModelFactory(AppModule.getContactsRepository(applicationContext))
                                )
                                val contactsState by contactsViewModel.state.collectAsStateWithLifecycle()

                                ContactsScreen(
                                    state = contactsState,
                                    onRefresh = { contactsViewModel.refreshContacts() },
                                    onCreateContact = { contactsViewModel.createContact() },
                                    onUpdateForm = { first, last, email, phone, title ->
                                        contactsViewModel.updateForm(first, last, email, phone, title)
                                    },
                                    onToggleDialog = { contactsViewModel.toggleCreateDialog(it) },
                                    onDeleteContact = { contactsViewModel.deleteContact(it) },
                                    onDismissMessage = { contactsViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.Deals.route) {
                                val dealsViewModel = viewModel<DealsViewModel>(
                                    factory = DealsViewModelFactory(AppModule.getDealsRepository(applicationContext))
                                )
                                val dealsState by dealsViewModel.state.collectAsStateWithLifecycle()

                                DealsScreen(
                                    state = dealsState,
                                    onRefresh = { dealsViewModel.refreshDeals() },
                                    onCreateDeal = { dealsViewModel.createDeal() },
                                    onUpdateForm = { name, amount, stage -> dealsViewModel.updateForm(name, amount, stage) },
                                    onToggleDialog = { dealsViewModel.toggleCreateDialog(it) },
                                    onUpdateStage = { id, stageId, stageName -> dealsViewModel.updateDealStage(id, stageId, stageName) },
                                    onDeleteDeal = { dealsViewModel.deleteDeal(it) },
                                    onDismissMessage = { dealsViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.Accounts.route) {
                                val accountsViewModel = viewModel<AccountsViewModel>(
                                    factory = AccountsViewModelFactory(AppModule.getAccountsRepository(applicationContext))
                                )
                                val accountsState by accountsViewModel.state.collectAsStateWithLifecycle()

                                AccountsScreen(
                                    state = accountsState,
                                    onRefresh = { accountsViewModel.refreshAccounts() },
                                    onSubmitForm = { accountsViewModel.submitForm() },
                                    onUpdateForm = { name, domain, industry, phone, status ->
                                        accountsViewModel.updateForm(name, domain, industry, phone, status)
                                    },
                                    onToggleDialog = { show, id -> accountsViewModel.toggleDialog(show, id) },
                                    onDeleteAccount = { accountsViewModel.deleteAccount(it) },
                                    onDismissMessage = { accountsViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.Settings.route) {
                                val settingsViewModel = viewModel<SettingsViewModel>(
                                    factory = SettingsViewModelFactory(AppModule.getOrganizationRepository(applicationContext))
                                )
                                val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()

                                LaunchedEffect(settingsState.shouldRefreshAuth) {
                                    if (settingsState.shouldRefreshAuth) {
                                        authViewModel.checkAuthState()
                                        settingsViewModel.clearAuthRefreshFlag()
                                    }
                                }

                                SettingsScreen(
                                    state = settingsState,
                                    userName = authState.user?.name ?: "User",
                                    userEmail = authState.user?.email ?: "",
                                    onRefresh = { settingsViewModel.refreshData() },
                                    onCreateOrg = { settingsViewModel.createOrganization() },
                                    onJoinOrg = { settingsViewModel.joinOrganization() },
                                    onUpdateCreateForm = { name, domain -> settingsViewModel.updateCreateOrgForm(name, domain) },
                                    onUpdateJoinForm = { email, name -> settingsViewModel.updateJoinOrgForm(email, name) },
                                    onSwitchTenant = { settingsViewModel.switchTenant(it) },
                                    onLogout = { authViewModel.signOut() },
                                    onDismissMessage = { settingsViewModel.dismissMessage() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check auth state when app resumes
        authViewModel.checkAuthState()
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

@Composable
private fun LighthouseBottomBar(
    currentDestination: NavDestination?,
    onNavigate: (MainDestination) -> Unit
) {
    NavigationBar {
        MainDestination.bottomNavItems.forEach { destination ->
            val selected = currentDestination?.route == destination.route
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(destination) },
                icon = {
                    Icon(
                        painter = painterResource(id = destination.iconRes),
                        contentDescription = stringResource(id = destination.labelRes)
                    )
                },
                label = { Text(text = stringResource(id = destination.labelRes)) }
            )
        }
    }
}

private fun shouldShowBottomBar(destination: NavDestination?): Boolean {
    return MainDestination.bottomNavItems.any { it.route == destination?.route }
}
