package com.project.lighthouse.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.project.lighthouse.R

sealed class MainDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int
) {
    data object Dashboard : MainDestination("dashboard", R.string.nav_dashboard, R.drawable.ic_dashboard)
    data object Leads : MainDestination("leads", R.string.nav_leads, R.drawable.ic_leads)
    data object Contacts : MainDestination("contacts", R.string.nav_contacts, R.drawable.ic_contacts)
    data object Deals : MainDestination("deals", R.string.nav_deals, R.drawable.ic_deals)
    data object Accounts : MainDestination("accounts", R.string.nav_accounts, R.drawable.ic_accounts)
    data object Settings : MainDestination("settings", R.string.nav_settings, R.drawable.ic_settings)

    companion object {
        val bottomNavItems = listOf(Dashboard, Leads, Contacts, Deals, Accounts, Settings)
    }
}

