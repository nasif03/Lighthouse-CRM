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
    data object Settings : MainDestination("settings", R.string.nav_settings, R.drawable.ic_settings)
    data object Gmail : MainDestination("gmail", R.string.nav_gmail, R.drawable.ic_gmail)
    data object Meetings : MainDestination("meetings", R.string.nav_meetings, R.drawable.ic_meetings)
    data object Chat : MainDestination("chat", R.string.nav_chat, R.drawable.ic_chat)

    companion object {
        val bottomNavItems = listOf(Dashboard, Leads, Contacts, Deals, Settings, Gmail, Meetings, Chat)
    }
}

