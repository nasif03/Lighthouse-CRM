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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.identity.Identity
import com.project.lighthouse.authentication.GoogleAuthUiClient
import com.project.lighthouse.authentication.SignInScreen
import com.project.lighthouse.authentication.SignInViewModel
import com.project.lighthouse.di.AppModule
import com.project.lighthouse.ui.auth.AuthViewModel
import com.project.lighthouse.ui.auth.AuthViewModelFactory
import com.project.lighthouse.ui.common.PlaceholderScreen
import com.project.lighthouse.ui.accounts.AccountsScreen
import com.project.lighthouse.ui.accounts.AccountsViewModel
import com.project.lighthouse.ui.accounts.AccountsViewModelFactory
import com.project.lighthouse.ui.accounts.AccountDetailScreen
import com.project.lighthouse.ui.accounts.AccountDetailViewModel
import com.project.lighthouse.ui.accounts.AccountDetailViewModelFactory
import com.project.lighthouse.ui.calendar.CalendarScreen
import com.project.lighthouse.ui.calendar.CalendarViewModel
import com.project.lighthouse.ui.calendar.CalendarViewModelFactory
import com.project.lighthouse.ui.contacts.ContactsScreen
import com.project.lighthouse.ui.fireflies.FirefliesScreen
import com.project.lighthouse.ui.fireflies.FirefliesViewModel
import com.project.lighthouse.ui.fireflies.FirefliesViewModelFactory
import com.project.lighthouse.ui.supportai.SupportAIScreen
import com.project.lighthouse.ui.supportai.SupportAIViewModel
import com.project.lighthouse.ui.supportai.SupportAIViewModelFactory
import com.project.lighthouse.ui.tickets.TicketsScreen
import com.project.lighthouse.ui.tickets.TicketsViewModel
import com.project.lighthouse.ui.tickets.TicketsViewModelFactory
import com.project.lighthouse.ui.tickets.TicketDetailScreen
import com.project.lighthouse.ui.tickets.TicketDetailViewModel
import com.project.lighthouse.ui.tickets.TicketDetailViewModelFactory
import com.project.lighthouse.ui.tickets.CreateTicketScreen
import com.project.lighthouse.ui.tickets.SubmitTicketScreen
import com.project.lighthouse.ui.tickets.SubmitTicketViewModel
import com.project.lighthouse.ui.tickets.SubmitTicketViewModelFactory
import com.project.lighthouse.ui.administration.AdministrationScreen
import com.project.lighthouse.ui.administration.AdministrationViewModel
import com.project.lighthouse.ui.administration.AdministrationViewModelFactory
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
import com.project.lighthouse.ui.navigation.CollapsibleBottomBar
import com.project.lighthouse.ui.settings.SettingsScreen
import com.project.lighthouse.ui.settings.SettingsViewModel
import com.project.lighthouse.ui.settings.SettingsViewModelFactory
import com.project.lighthouse.ui.gmail.GmailScreen
import com.project.lighthouse.ui.gmail.GmailViewModel
import com.project.lighthouse.ui.gmail.GmailViewModelFactory
import com.project.lighthouse.ui.meetings.MeetingsScreen
import com.project.lighthouse.ui.meetings.MeetingsViewModel
import com.project.lighthouse.ui.meetings.MeetingsViewModelFactory
import com.project.lighthouse.ui.chat.ChatScreen
import com.project.lighthouse.ui.chat.ChatViewModel
import com.project.lighthouse.ui.chat.ChatViewModelFactory
import com.project.lighthouse.ui.theme.LighthouseTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

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
                            val showBottomBar = authState.isAuthenticated && this@MainActivity.shouldShowBottomBar(currentDestination)
                            Log.d(TAG, "Bottom bar visible? $showBottomBar route=${currentDestination?.route}")
                            if (showBottomBar) {
                                this@MainActivity.LighthouseBottomBar(
                                    currentDestination = currentDestination,
                                    onNavigate = { destination ->
                                        Log.d(TAG, "Navigating to ${destination.route}")
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
                        // Public route - SubmitTicket (no auth required)
                        composable(
                            route = MainDestination.SubmitTicket.route,
                            arguments = listOf(navArgument("orgId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val orgId = backStackEntry.arguments?.getString("orgId")
                            if (orgId != null) {
                                val submitTicketViewModel = viewModel<SubmitTicketViewModel>(
                                    factory = SubmitTicketViewModelFactory(
                                        AppModule.getTicketsRepository(applicationContext),
                                        orgId
                                    )
                                )
                                val submitTicketState by submitTicketViewModel.state.collectAsStateWithLifecycle()

                                SubmitTicketScreen(
                                    state = submitTicketState,
                                    onUpdateField = { field, value -> submitTicketViewModel.updateField(field, value) },
                                    onSubmitTicket = { submitTicketViewModel.submitTicket() },
                                    onResetSuccess = { submitTicketViewModel.resetSuccess() },
                                    onDismissMessage = { submitTicketViewModel.dismissMessage() }
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Organization ID is required",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                            composable("sign_in") {
                            if (authState.isAuthenticated) {
                                // Redirect to dashboard if already authenticated
                                LaunchedEffect(Unit) {
                                    navController.navigate(MainDestination.Dashboard.route) {
                                        popUpTo("sign_in") { inclusive = true }
                                    }
                                }
                            } else {
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

                                val signInScope = rememberCoroutineScope()

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
                                        navController.navigate(MainDestination.Dashboard.route) {
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
                                        signInScope.launch {
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
                                    onToggleDialog = { show, accountId -> accountsViewModel.toggleDialog(show, accountId) },
                                    onDeleteAccount = { accountsViewModel.deleteAccount(it) },
                                    onAccountClick = { accountId ->
                                        navController.navigate("account_detail/$accountId")
                                    },
                                    onDismissMessage = { accountsViewModel.dismissMessage() }
                                )
                            }

                            composable(
                                route = "account_detail/{accountId}",
                                arguments = listOf(navArgument("accountId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val accountId = backStackEntry.arguments?.getString("accountId") ?: return@composable
                                val accountDetailViewModel = viewModel<AccountDetailViewModel>(
                                    factory = AccountDetailViewModelFactory(
                                        AppModule.getAccountsRepository(applicationContext),
                                        accountId
                                    )
                                )
                                val accountDetailState by accountDetailViewModel.state.collectAsStateWithLifecycle()

                                AccountDetailScreen(
                                    state = accountDetailState,
                                    onRefresh = { accountDetailViewModel.loadAccountDetails() },
                                    onSelectTab = { accountDetailViewModel.selectTab(it) },
                                    onNavigateBack = { navController.popBackStack() },
                                    onDismissMessage = { accountDetailViewModel.dismissMessage() }
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

                            composable(MainDestination.Gmail.route) {
                                val gmailViewModel = viewModel<GmailViewModel>(
                                    factory = GmailViewModelFactory(AppModule.getGmailRepository(applicationContext))
                                )
                                val gmailState by gmailViewModel.state.collectAsStateWithLifecycle()

                                GmailScreen(
                                    state = gmailState,
                                    onRefresh = { gmailViewModel.refreshMessages() },
                                    onAuthenticate = { code, token, refreshToken -> 
                                        gmailViewModel.authenticate(code, token, refreshToken) 
                                    },
                                    onToggleSendEmailDialog = { gmailViewModel.toggleSendEmailDialog(it) },
                                    onUpdateSendEmailForm = { to, subject, body -> 
                                        gmailViewModel.updateSendEmailForm(to, subject, body) 
                                    },
                                    onSendEmail = { gmailViewModel.sendEmail() },
                                    onDismissMessage = { gmailViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.Meetings.route) {
                                val meetingsViewModel = viewModel<MeetingsViewModel>(
                                    factory = MeetingsViewModelFactory(AppModule.getMeetingsRepository(applicationContext))
                                )
                                val meetingsState by meetingsViewModel.state.collectAsStateWithLifecycle()

                                MeetingsScreen(
                                    state = meetingsState,
                                    onRefresh = { meetingsViewModel.refreshTranscripts() },
                                    onToggleCreateMeetingDialog = { meetingsViewModel.toggleCreateMeetingDialog(it) },
                                    onUpdateCreateMeetingForm = { title, startTime, endTime, description, attendees, timezone ->
                                        meetingsViewModel.updateCreateMeetingForm(title, startTime, endTime, description, attendees, timezone)
                                    },
                                    onAddAttendee = { meetingsViewModel.addAttendee(it) },
                                    onRemoveAttendee = { meetingsViewModel.removeAttendee(it) },
                                    onCreateMeeting = { meetingsViewModel.createMeeting() },
                                    onDismissMessage = { meetingsViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.Chat.route) {
                                val chatViewModel = viewModel<ChatViewModel>(
                                    factory = ChatViewModelFactory(AppModule.getChatRepository(applicationContext))
                                )
                                val chatState by chatViewModel.state.collectAsStateWithLifecycle()

                                ChatScreen(
                                    state = chatState,
                                    onRefreshChannels = { chatViewModel.refreshChannels() },
                                    onSelectChannel = { chatViewModel.selectChannel(it) },
                                    onToggleUserSelection = { show -> chatViewModel.toggleUserSelection(show) },
                                    onCreateChannelWithUser = { chatViewModel.createChannelWithUser(it) },
                                    onUpdateMessageInput = { chatViewModel.updateMessageInput(it) },
                                    onSendMessage = { chatViewModel.sendMessage() },
                                    onGoBackToChannelList = { chatViewModel.goBackToChannelList() },
                                    onDismissMessage = { chatViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.Calendar.route) {
                                val calendarViewModel = viewModel<CalendarViewModel>(
                                    factory = CalendarViewModelFactory(AppModule.getCalendarRepository(applicationContext))
                                )
                                val calendarState by calendarViewModel.state.collectAsStateWithLifecycle()

                                CalendarScreen(
                                    state = calendarState,
                                    onRefresh = { calendarViewModel.loadMeetings() },
                                    onDismissMessage = { calendarViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.Fireflies.route) {
                                val firefliesViewModel = viewModel<FirefliesViewModel>(
                                    factory = FirefliesViewModelFactory(AppModule.getFirefliesRepository(applicationContext))
                                )
                                val firefliesState by firefliesViewModel.state.collectAsStateWithLifecycle()

                                FirefliesScreen(
                                    state = firefliesState,
                                    onRefresh = { firefliesViewModel.loadTranscripts() },
                                    onSync = { firefliesViewModel.syncTranscripts() },
                                    onSelectTranscript = { transcript -> firefliesViewModel.selectTranscript(transcript) },
                                    onDismissMessage = { firefliesViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.SupportAI.route) {
                                val supportAIViewModel = viewModel<SupportAIViewModel>(
                                    factory = SupportAIViewModelFactory(AppModule.getSupportChatRepository(applicationContext))
                                )
                                val supportAIState by supportAIViewModel.state.collectAsStateWithLifecycle()

                                SupportAIScreen(
                                    state = supportAIState,
                                    onUpdateInput = { supportAIViewModel.updateInput(it) },
                                    onSendMessage = { supportAIViewModel.sendMessage() },
                                    onDismissMessage = { supportAIViewModel.dismissMessage() }
                                )
                            }

                            composable(MainDestination.Administration.route) {
                                val orgId = authState.user?.orgId as? String
                                if (orgId != null) {
                                    val administrationViewModel = viewModel<AdministrationViewModel>(
                                        factory = AdministrationViewModelFactory(
                                            AppModule.getEmployeesRepository(applicationContext),
                                            AppModule.getRolesRepository(applicationContext),
                                            orgId
                                        )
                                    )
                                    val administrationState by administrationViewModel.state.collectAsStateWithLifecycle()

                                    AdministrationScreen(
                                        state = administrationState,
                                        onUpdateNewEmployeeName = { administrationViewModel.updateNewEmployeeName(it) },
                                        onUpdateNewEmployeeEmail = { administrationViewModel.updateNewEmployeeEmail(it) },
                                        onToggleRoleSelection = { administrationViewModel.toggleRoleSelection(it) },
                                        onAddEmployee = { administrationViewModel.addEmployee() },
                                        onStartEditingEmployee = { administrationViewModel.startEditingEmployee(it) },
                                        onCancelEditingEmployee = { administrationViewModel.cancelEditingEmployee() },
                                        onToggleEditingRoleSelection = { administrationViewModel.toggleEditingRoleSelection(it) },
                                        onUpdateEmployeeRoles = { administrationViewModel.updateEmployeeRoles() },
                                        onUpdateNewRoleName = { administrationViewModel.updateNewRoleName(it) },
                                        onTogglePermission = { administrationViewModel.togglePermission(it) },
                                        onAddRole = { administrationViewModel.addRole() },
                                        onDeleteRole = { administrationViewModel.deleteRole(it) },
                                        onDismissMessage = { administrationViewModel.dismissMessage() }
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Please select an organization to manage employees and roles.",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            composable(MainDestination.Tickets.route) {
                                val ticketsViewModel = viewModel<TicketsViewModel>(
                                    factory = TicketsViewModelFactory(AppModule.getTicketsRepository(applicationContext))
                                )
                                val ticketsState by ticketsViewModel.state.collectAsStateWithLifecycle()
                                val orgId = authState.user?.orgId as? String

                                TicketsScreen(
                                    state = ticketsState,
                                    orgId = orgId,
                                    onRefresh = { ticketsViewModel.refreshTickets() },
                                    onToggleCreateDialog = { ticketsViewModel.toggleCreateDialog(it) },
                                    onUpdateCreateForm = { name, email, phone, subject, description, priority, category ->
                                        ticketsViewModel.updateCreateTicketForm(name, email, phone, subject, description, priority, category)
                                    },
                                    onCreateTicket = { ticketsViewModel.createTicket(it) },
                                    onToggleUpdateDialog = { show, ticket -> ticketsViewModel.toggleUpdateDialog(show, ticket) },
                                    onUpdateTicket = { ticketId, status, priority, assignedTo, category ->
                                        ticketsViewModel.updateTicket(ticketId, status, priority, assignedTo, category)
                                    },
                                    onSetFilter = { status, priority -> ticketsViewModel.setFilter(status, priority) },
                                    onSetSearchQuery = { ticketsViewModel.setSearchQuery(it) },
                                    onNavigateToCreateTicket = { navController.navigate(MainDestination.CreateTicket.route) },
                                    onDismissMessage = { ticketsViewModel.dismissMessage() },
                                    onViewDetails = { ticketId -> navController.navigate("tickets/$ticketId") }
                                )
                            }

                            composable(
                                route = MainDestination.TicketDetail.route,
                                arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val ticketId = backStackEntry.arguments?.getString("ticketId")
                                if (ticketId != null) {
                                    val ticketDetailViewModel = viewModel<TicketDetailViewModel>(
                                        factory = TicketDetailViewModelFactory(
                                            AppModule.getTicketsRepository(applicationContext),
                                            ticketId,
                                            authState.user?.name ?: "User"
                                        )
                                    )
                                    val ticketDetailState by ticketDetailViewModel.state.collectAsStateWithLifecycle()

                                    TicketDetailScreen(
                                        state = ticketDetailState,
                                        onRefresh = { ticketDetailViewModel.loadTicket() },
                                        onUpdateStatus = { ticketDetailViewModel.updateStatus(it) },
                                        onUpdatePriority = { ticketDetailViewModel.updatePriority(it) },
                                        onAssignTicket = { ticketDetailViewModel.assignTicket(it) },
                                        onAddComment = { content, isInternal -> ticketDetailViewModel.addComment(content, isInternal) },
                                        onUpdateNewComment = { ticketDetailViewModel.updateNewComment(it) },
                                        onToggleInternalNote = { ticketDetailViewModel.toggleInternalNote(it) },
                                        onToggleAssignModal = { ticketDetailViewModel.toggleAssignModal(it) },
                                        onToggleStatusModal = { ticketDetailViewModel.toggleStatusModal(it) },
                                        onUpdateSelectedAssignee = { ticketDetailViewModel.updateSelectedAssignee(it) },
                                        onUpdateSelectedStatus = { ticketDetailViewModel.updateSelectedStatus(it) },
                                    onNavigateBack = { navController.popBackStack() },
                                    onDismissMessage = { ticketDetailViewModel.dismissMessage() }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.popBackStack()
                                }
                            }
                        }

                            composable(MainDestination.CreateTicket.route) {
                                val ticketsViewModel = viewModel<TicketsViewModel>(
                                    factory = TicketsViewModelFactory(AppModule.getTicketsRepository(applicationContext))
                                )
                                val ticketsState by ticketsViewModel.state.collectAsStateWithLifecycle()
                                
                                val createTicketUser = authState.user
                                val createTicketOrgId = createTicketUser?.orgId as? String
                                
                                // Initialize form with user data if not already set
                                LaunchedEffect(createTicketUser) {
                                    if (ticketsState.createTicketFormState.name.isEmpty() && 
                                        ticketsState.createTicketFormState.email.isEmpty() &&
                                        createTicketUser != null) {
                                        ticketsViewModel.updateCreateTicketForm(
                                            name = createTicketUser.name ?: "",
                                            email = createTicketUser.email ?: ""
                                        )
                                    }
                                }

                                LaunchedEffect(ticketsState.createdTicketId) {
                                    ticketsState.createdTicketId?.let {
                                        navController.navigate("tickets/$it") {
                                            popUpTo(MainDestination.Tickets.route) { inclusive = false }
                                        }
                                        ticketsViewModel.clearCreatedTicketId()
                                    }
                                }

                                CreateTicketScreen(
                                    state = ticketsState.createTicketFormState,
                                    orgId = createTicketOrgId,
                                    userName = createTicketUser?.name,
                                    userEmail = createTicketUser?.email,
                                    errorMessage = ticketsState.errorMessage,
                                    infoMessage = ticketsState.infoMessage,
                                    onUpdateField = { name, email, phone, subject, description, priority, category ->
                                        ticketsViewModel.updateCreateTicketForm(name, email, phone, subject, description, priority, category)
                                    },
                                    onSubmit = { ticketsViewModel.createTicket(it) },
                                    onNavigateBack = { navController.popBackStack() },
                                    onNavigateToTicket = { ticketId ->
                                        navController.navigate("tickets/$ticketId") {
                                            popUpTo(MainDestination.Tickets.route) { inclusive = false }
                                        }
                                    },
                                    onDismissMessage = { ticketsViewModel.dismissMessage() },
                                    createdTicketId = ticketsState.createdTicketId
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
        authViewModel.checkAuthState()
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
                    label = {
                        Text(
                            text = stringResource(id = destination.labelRes),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    },
                    alwaysShowLabel = true
                )
            }
        }
    }

    private fun shouldShowBottomBar(destination: NavDestination?): Boolean {
        return MainDestination.bottomNavItems.any { it.route == destination?.route }
    }
}
