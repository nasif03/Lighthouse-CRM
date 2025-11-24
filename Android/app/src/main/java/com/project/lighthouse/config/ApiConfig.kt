package com.project.lighthouse.config

object ApiConfig {
    // Base URL configuration:
    // - For Android Emulator: use "http://10.0.2.2:3000/"
    // - For Physical Device: use "http://YOUR_COMPUTER_IP:3000/"
    //   Replace YOUR_COMPUTER_IP with your computer's local IP address
    //   Find it by running: ipconfig (Windows) or ifconfig (Linux/Mac)
    //   Look for IPv4 address (usually 192.168.x.x or 10.x.x.x)
    // Note: Retrofit requires base URL to end with /
    const val BASE_URL = "http://192.168.0.209:3000/"  // TODO: Update with your actual IP
    
    // API endpoints
    const val API_PREFIX = "/api"
}

