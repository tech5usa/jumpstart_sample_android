package com.example.iwsgmisampleapp

import android.util.Log
import com.iwsinc.ims.listeners.IMSResponseListener
import com.iwsinc.ims.response.IMSResponse
import kotlinx.coroutines.sync.Semaphore


class BlockingNativeMessageResponseListener : IMSResponseListener {
    var semaphore = Semaphore(1)
    var lastResponse: IMSResponse? = null
    override fun onResponse(imsr: IMSResponse) {
        Log.d("LISTENER",".onResponse($imsr): releasing semaphore...")
        lastResponse = imsr
        semaphore.release()
    }

    suspend fun requestAndWaitForPermit() {
        Log.d("LISTENER", ".requestAndWaitForPermit(): acquiring semaphore permit, suspending until available...")
        semaphore.acquire()
        Log.d("LISTENER", ".requestAndWaitForPermit(): acquired semaphore permit, returning...")
    }

    override fun toString(): String {
        return if (lastResponse != null) {
            lastResponse!!.toString()
        } else "(null)"
    }

    fun releasePermit() { if (semaphore.availablePermits > 0) { semaphore.release() } }
}
