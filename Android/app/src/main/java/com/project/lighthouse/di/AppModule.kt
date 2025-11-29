package com.project.lighthouse.di

import android.content.Context
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.local.TokenManager
import com.project.lighthouse.data.repository.AccountsRepository
import com.project.lighthouse.data.repository.AuthRepository
import com.project.lighthouse.data.repository.CalendarRepository
import com.project.lighthouse.data.repository.ContactsRepository
import com.project.lighthouse.data.repository.FirefliesRepository
import com.project.lighthouse.data.repository.DashboardRepository
import com.project.lighthouse.data.repository.DealsRepository
import com.project.lighthouse.data.repository.ChatRepository
import com.project.lighthouse.data.repository.GmailRepository
import com.project.lighthouse.data.repository.SupportChatRepository
import com.project.lighthouse.data.repository.JiraRepository
import com.project.lighthouse.data.repository.LeadsRepository
import com.project.lighthouse.data.repository.MeetingsRepository
import com.project.lighthouse.data.repository.EmployeesRepository
import com.project.lighthouse.data.repository.OrganizationRepository
import com.project.lighthouse.data.repository.RolesRepository
import com.project.lighthouse.data.repository.TicketsRepository
import com.project.lighthouse.ui.auth.AuthViewModel

object AppModule {
    private var tokenManager: TokenManager? = null
    private var authRepository: AuthRepository? = null
    private var dashboardRepository: DashboardRepository? = null
    private var leadsRepository: LeadsRepository? = null
    private var contactsRepository: ContactsRepository? = null
    private var dealsRepository: DealsRepository? = null
    private var accountsRepository: AccountsRepository? = null
    private var calendarRepository: CalendarRepository? = null
    private var firefliesRepository: FirefliesRepository? = null
    private var organizationRepository: OrganizationRepository? = null
    private var gmailRepository: GmailRepository? = null
    private var meetingsRepository: MeetingsRepository? = null
    private var jiraRepository: JiraRepository? = null
    private var ticketsRepository: TicketsRepository? = null
    private var chatRepository: ChatRepository? = null
    private var supportChatRepository: SupportChatRepository? = null
    private var employeesRepository: EmployeesRepository? = null
    private var rolesRepository: RolesRepository? = null

    fun initialize(context: Context) {
        // Initialize TokenManager
        tokenManager = TokenManager(context)
        
        // Initialize ApiClient with TokenManager
        ApiClient.initialize(tokenManager!!)
        
        // Initialize repositories
        authRepository = AuthRepository(tokenManager!!)
        dashboardRepository = DashboardRepository()
        leadsRepository = LeadsRepository()
        contactsRepository = ContactsRepository()
        dealsRepository = DealsRepository()
        accountsRepository = AccountsRepository()
        calendarRepository = CalendarRepository()
        firefliesRepository = FirefliesRepository()
        organizationRepository = OrganizationRepository()
        gmailRepository = GmailRepository()
        meetingsRepository = MeetingsRepository()
        jiraRepository = JiraRepository()
        ticketsRepository = TicketsRepository()
        chatRepository = ChatRepository()
        supportChatRepository = SupportChatRepository()
        employeesRepository = EmployeesRepository()
        rolesRepository = RolesRepository()
    }

    fun getTokenManager(context: Context): TokenManager {
        if (tokenManager == null) {
            initialize(context)
        }
        return tokenManager!!
    }

    fun getAuthRepository(context: Context): AuthRepository {
        if (authRepository == null) {
            initialize(context)
        }
        return authRepository!!
    }

    fun getDashboardRepository(context: Context): DashboardRepository {
        if (dashboardRepository == null) {
            initialize(context)
        }
        return dashboardRepository!!
    }

    fun getLeadsRepository(context: Context): LeadsRepository {
        if (leadsRepository == null) {
            initialize(context)
        }
        return leadsRepository!!
    }

    fun getContactsRepository(context: Context): ContactsRepository {
        if (contactsRepository == null) {
            initialize(context)
        }
        return contactsRepository!!
    }

    fun getDealsRepository(context: Context): DealsRepository {
        if (dealsRepository == null) {
            initialize(context)
        }
        return dealsRepository!!
    }

    fun getAccountsRepository(context: Context): AccountsRepository {
        if (accountsRepository == null) {
            initialize(context)
        }
        return accountsRepository!!
    }

    fun getCalendarRepository(context: Context): CalendarRepository {
        if (calendarRepository == null) {
            initialize(context)
        }
        return calendarRepository!!
    }

    fun getFirefliesRepository(context: Context): FirefliesRepository {
        if (firefliesRepository == null) {
            initialize(context)
        }
        return firefliesRepository!!
    }

    fun getOrganizationRepository(context: Context): OrganizationRepository {
        if (organizationRepository == null) {
            initialize(context)
        }
        return organizationRepository!!
    }

    fun getGmailRepository(context: Context): GmailRepository {
        if (gmailRepository == null) {
            initialize(context)
        }
        return gmailRepository!!
    }

    fun getMeetingsRepository(context: Context): MeetingsRepository {
        if (meetingsRepository == null) {
            initialize(context)
        }
        return meetingsRepository!!
    }

    fun getJiraRepository(context: Context): JiraRepository {
        if (jiraRepository == null) {
            initialize(context)
        }
        return jiraRepository!!
    }

    fun getTicketsRepository(context: Context): TicketsRepository {
        if (ticketsRepository == null) {
            initialize(context)
        }
        return ticketsRepository!!
    }

    fun getChatRepository(context: Context): ChatRepository {
        if (chatRepository == null) {
            initialize(context)
        }
        return chatRepository!!
    }

    fun getSupportChatRepository(context: Context): SupportChatRepository {
        if (supportChatRepository == null) {
            initialize(context)
        }
        return supportChatRepository!!
    }

    fun getEmployeesRepository(context: Context): EmployeesRepository {
        if (employeesRepository == null) {
            initialize(context)
        }
        return employeesRepository!!
    }

    fun getRolesRepository(context: Context): RolesRepository {
        if (rolesRepository == null) {
            initialize(context)
        }
        return rolesRepository!!
    }

    fun getAuthViewModel(context: Context): AuthViewModel {
        val tokenManager = getTokenManager(context)
        val authRepository = getAuthRepository(context)
        return AuthViewModel(authRepository, tokenManager)
    }
}

