package com.wellbeing.service

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process

/**
 * Utility to verify whether PACKAGE_USAGE_STATS permission has been granted.
 *
 * Implements Object Calisthenics: zero 'else', early returns, single-level indentation.
 */
object UsageAccessChecker {

    fun isUsageAccessGranted(context: Context): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = checkAppOpsMode(appOpsManager, context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @Suppress("DEPRECATION")
    private fun checkAppOpsMode(appOpsManager: AppOpsManager, packageName: String): Int {
        val uid = Process.myUid()
        val op = AppOpsManager.OPSTR_GET_USAGE_STATS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return appOpsManager.unsafeCheckOpNoThrow(op, uid, packageName)
        }
        return appOpsManager.checkOpNoThrow(op, uid, packageName)
    }
}
