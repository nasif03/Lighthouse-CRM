package com.project.lighthouse.di

import android.content.Context
import com.project.lighthouse.data.api.ApiClient
import com.project.lighthouse.data.local.TokenManager
import com.project.lighthouse.data.repository.AccountsRepository
import com.project.lighthouse.data.repository.AuthRepository
import com.project.lighthouse.data.repository.ContactsRepository
import com.project.lighthouse.data.repository.DashboardRepository
import com.project.lighthouse.data.repository.DealsRepository
import com.project.lighthouse.data.repository.LeadsRepository
import com.project.lighthouse.data.repository.OrganizationRepository
import com.project.lighthouse.ui.auth.AuthViewModel

object AppModule {
    private var tokenManager: TokenManager? = null
    private var authRepository: AuthRepository? = null
    private var dashboardRepository: DashboardRepository? = null
    private var leadsRepository: LeadsRepository? = null
    private var contactsRepository: ContactsRepository? = null
    private var dealsRepository: DealsRepository? = null
    private var accountsRepository: AccountsRepository? = null
    private var organizationRepository: OrganizationRepository? = null

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
        organizationRepository = OrganizationRepository()
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

    fun getOrganizationRepository(context: Context): OrganizationRepository {
        if (organizationRepository == null) {
            initialize(context)
        }
        return organizationRepository!!
    }

    fun getAuthViewModel(context: Context): AuthViewModel {
        val tokenManager = getTokenManager(context)
        val authRepository = getAuthRepository(context)
        return AuthViewModel(authRepository, tokenManager)
    }
}

