package com.example.iwsgmisampleapp

import com.iwsinc.ims.api.Enrollment
import com.iwsinc.ims.api.MessagesServiceManager
import com.iwsinc.ims.api.Profile

class ProfileDetailsManager(private val profile: Profile, private val messagesServiceManager: MessagesServiceManager) {

    var enrollsForAccount: List<Enrollment> = listOf()
    val enrollsTenantMap: MutableMap<String, MutableList<Enrollment>> = mutableMapOf()
    val enrollsTenantFlat: MutableList<Enrollment> = mutableListOf()
    var sectionHeaders: List<String> = listOf()

    init {
        refresh()
    }

    fun refresh() {
        enrollsForAccount = messagesServiceManager.enrolls.filter {
            (it.profile!!.email == profile.email)
                    && (it.profile!!.configuration.gmiServerUrl == profile.configuration.gmiServerUrl)
                    && (it.captureType != null)
                    && (it.captureType!!.isNotEmpty())
        }

        for (enrollment in enrollsForAccount) {

            //TODO: Change to `enrollment.tenant` once this field is added to the GMI SDK
            val tenantCode = enrollment.profile!!.configuration.defaultTenantCode

            enrollsTenantFlat.add(enrollment)
            if (enrollsTenantMap[tenantCode] == null) {
                enrollsTenantMap[tenantCode] = mutableListOf()
            }
            enrollsTenantMap[tenantCode]!!.add(enrollment)
        }

        sectionHeaders = enrollsTenantMap.keys.sortedWith(Comparator { o1: String, o2: String ->
            when {
                o1 == "ImageWare" -> -1
                o2 == "ImageWare" -> 1
                o1 > o2 -> 1
                o1 < o2 -> -1
                else -> 0
            }
        })
    }
}
