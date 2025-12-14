package com.codecontext.enterprise

object LicenseManager {
    enum class Tier {
        FREE,
        PRO,
        ENTERPRISE
    }

    fun getTier(apiKey: String?): Tier {
        return when (apiKey) {
            "sk-enterprise-demo" -> Tier.ENTERPRISE
            "sk-pro-demo" -> Tier.PRO
            else -> Tier.FREE
        }
    }

    fun checkLimit(tier: Tier, currentProcessing: Int): Boolean {
        return when (tier) {
            Tier.FREE -> currentProcessing < 1
            Tier.PRO -> currentProcessing < 5
            Tier.ENTERPRISE -> true
        }
    }
}
