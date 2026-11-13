package cz.fittrack.app

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

/**
 * Read-only bridge to Android Health Connect. Samsung Health (Galaxy Watch) writes into
 * Health Connect once the user enables sharing in Samsung Health settings.
 */
@CapacitorPlugin(name = "Health")
class HealthPlugin : Plugin() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val provider = "com.google.android.apps.healthdata"

    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    private fun sdkStatus() = HealthConnectClient.getSdkStatus(context, provider)
    private fun client() = HealthConnectClient.getOrCreate(context, provider)

    @PluginMethod
    fun status(call: PluginCall) {
        val status = sdkStatus()
        val ret = JSObject()
        ret.put("available", status == HealthConnectClient.SDK_AVAILABLE)
        ret.put("needsInstall", status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            ret.put("granted", false)
            call.resolve(ret)
            return
        }
        scope.launch {
            try {
                val granted = client().permissionController.getGrantedPermissions()
                ret.put("granted", granted.containsAll(permissions))
                ret.put("grantedCount", granted.intersect(permissions).size)
                call.resolve(ret)
            } catch (e: Exception) {
                call.reject(e.message ?: "Health Connect status failed", e)
            }
        }
    }

    @PluginMethod
    fun authorize(call: PluginCall) {
        if (sdkStatus() != HealthConnectClient.SDK_AVAILABLE) {
            call.reject("Health Connect není v telefonu dostupný.")
            return
        }
        val intent = PermissionController.createRequestPermissionResultContract(provider)
            .createIntent(context, permissions)
        startActivityForResult(call, intent, "authorizeResult")
    }

    @ActivityCallback
    fun authorizeResult(call: PluginCall?, result: ActivityResult) {
        if (call == null) return
        scope.launch {
            try {
                val granted = client().permissionController.getGrantedPermissions()
                val ret = JSObject()
                ret.put("granted", granted.containsAll(permissions))
                ret.put("grantedCount", granted.intersect(permissions).size)
                call.resolve(ret)
            } catch (e: Exception) {
                call.reject(e.message ?: "Health Connect authorization failed", e)
            }
        }
    }

    /** Opens Play Store (install/update Health Connect) or Health Connect settings. */
    @PluginMethod
    fun openHealthConnect(call: PluginCall) {
        val intent = if (sdkStatus() == HealthConnectClient.SDK_AVAILABLE) {
            Intent(HealthConnectClient.getHealthConnectManageDataIntent(context, provider))
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$provider&url=healthconnect%3A%2F%2Fonboarding"))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
            call.resolve()
        } catch (e: Exception) {
            call.reject("Nepodařilo se otevřít Health Connect.", e)
        }
    }

    @PluginMethod
    fun readSummary(call: PluginCall) {
        val days = (call.getInt("days") ?: 7).coerceIn(1, 30)
        scope.launch {
            try {
                val c = client()
                val granted = c.permissionController.getGrantedPermissions()
                val zone = ZoneId.systemDefault()
                val now = Instant.now()
                val today = LocalDate.now(zone)
                val startOfToday = today.atStartOfDay(zone).toInstant()
                val ret = JSObject()

                if (HealthPermission.getReadPermission(StepsRecord::class) in granted) {
                    val todayAgg = c.aggregate(AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(startOfToday, now)))
                    ret.put("stepsToday", todayAgg[StepsRecord.COUNT_TOTAL] ?: 0L)
                    val byDay = c.aggregateGroupByPeriod(
                        AggregateGroupByPeriodRequest(
                            setOf(StepsRecord.COUNT_TOTAL),
                            TimeRangeFilter.between(today.minusDays((days - 1).toLong()).atStartOfDay(), today.plusDays(1).atStartOfDay()),
                            Period.ofDays(1),
                        )
                    )
                    val arr = JSArray()
                    byDay.forEach {
                        val o = JSObject()
                        o.put("date", it.startTime.toLocalDate().toString())
                        o.put("steps", it.result[StepsRecord.COUNT_TOTAL] ?: 0L)
                        arr.put(o)
                    }
                    ret.put("stepsByDay", arr)
                }

                if (HealthPermission.getReadPermission(HeartRateRecord::class) in granted) {
                    val hr = c.aggregate(
                        AggregateRequest(
                            setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_MAX),
                            TimeRangeFilter.between(startOfToday, now),
                        )
                    )
                    hr[HeartRateRecord.BPM_AVG]?.let { ret.put("heartRateAvg", it) }
                    hr[HeartRateRecord.BPM_MIN]?.let { ret.put("heartRateMin", it) }
                    hr[HeartRateRecord.BPM_MAX]?.let { ret.put("heartRateMax", it) }
                }

                if (HealthPermission.getReadPermission(SleepSessionRecord::class) in granted) {
                    // Last night: sessions ending between yesterday 18:00 and now.
                    val sleep = c.aggregate(
                        AggregateRequest(
                            setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                            TimeRangeFilter.between(startOfToday.minus(Duration.ofHours(6)), now),
                        )
                    )
                    sleep[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.let { ret.put("sleepMinutes", it.toMinutes()) }
                }

                if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) in granted) {
                    val from = today.minusDays((days - 1).toLong()).atStartOfDay(zone).toInstant()
                    val records = c.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.between(from, now))).records
                    val arr = JSArray()
                    records.forEach { r ->
                        val (kind, label) = classify(r.exerciseType)
                        val o = JSObject()
                        o.put("id", r.metadata.id)
                        o.put("title", r.title?.takeIf { it.isNotBlank() } ?: label)
                        o.put("kind", kind)
                        o.put("minutes", Duration.between(r.startTime, r.endTime).toMinutes().coerceAtLeast(1))
                        o.put("date", r.startTime.atZone(zone).toLocalDate().toString())
                        o.put("source", r.metadata.dataOrigin.packageName)
                        arr.put(o)
                    }
                    ret.put("exercises", arr)
                }

                ret.put("grantedCount", granted.intersect(permissions).size)
                call.resolve(ret)
            } catch (e: Exception) {
                call.reject(e.message ?: "Čtení z Health Connect selhalo.", e)
            }
        }
    }

    private fun classify(type: Int): Pair<String, String> = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING,
        ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS -> "strength" to "Silový trénink"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "mobility" to "Jóga"
        ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "mobility" to "Pilates"
        ExerciseSessionRecord.EXERCISE_TYPE_STRETCHING -> "mobility" to "Protažení"
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "cardio" to "Běh"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "cardio" to "Chůze"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "cardio" to "Turistika"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "cardio" to "Kolo"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "cardio" to "Plavání"
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "cardio" to "Eliptický trenažér"
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "cardio" to "Veslování"
        else -> "cardio" to "Aktivita z hodinek"
    }
}
