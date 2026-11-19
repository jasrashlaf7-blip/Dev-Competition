package com.olympiades.firststep.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.olympiades.firststep.domain.model.AppLanguage
import com.olympiades.firststep.domain.model.AppThemeMode
import com.olympiades.firststep.domain.model.OfferFilterCriteria
import com.olympiades.firststep.domain.model.OfferSortOption
import com.olympiades.firststep.domain.security.PinSecurityHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.firstStepDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "firststep_preferences"
)

data class UserPreferencesState(
    val language: AppLanguage = AppLanguage.FR,
    val themeMode: AppThemeMode = AppThemeMode.LIGHT,
    val lastFilters: OfferFilterCriteria = OfferFilterCriteria(),
    val pinLockEnabled: Boolean = false,
    val pinSalt: String = "",
    val pinHash: String = ""
)

class UserPreferencesDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val THEME_MODE = stringPreferencesKey("app_theme_mode")
        val FILTER_QUERY = stringPreferencesKey("filter_query")
        val FILTER_PAID_ONLY = booleanPreferencesKey("filter_paid_only")
        val FILTER_OPEN_ONLY = booleanPreferencesKey("filter_open_only")
        val FILTER_MIN_STIPEND = intPreferencesKey("filter_min_stipend")
        val FILTER_DOMAIN_ID = longPreferencesKey("filter_domain_id")
        val FILTER_LOCATION_ID = longPreferencesKey("filter_location_id")
        val FILTER_SORT = stringPreferencesKey("filter_sort")
        val PIN_ENABLED = booleanPreferencesKey("pin_lock_enabled")
        val PIN_SALT = stringPreferencesKey("pin_salt")
        val PIN_HASH = stringPreferencesKey("pin_hash")
    }

    val preferencesFlow: Flow<UserPreferencesState> = context.firstStepDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val language = when (prefs[Keys.LANGUAGE]) {
                AppLanguage.AR.code -> AppLanguage.AR
                else -> AppLanguage.FR
            }
            val theme = when (prefs[Keys.THEME_MODE]) {
                AppThemeMode.DARK.key -> AppThemeMode.DARK
                else -> AppThemeMode.LIGHT
            }
            val sortOption = prefs[Keys.FILTER_SORT]?.let { name ->
                runCatching { OfferSortOption.valueOf(name) }.getOrNull()
            } ?: OfferSortOption.NEWEST

            UserPreferencesState(
                language = language,
                themeMode = theme,
                lastFilters = OfferFilterCriteria(
                    query = prefs[Keys.FILTER_QUERY] ?: "",
                    paidOnly = prefs[Keys.FILTER_PAID_ONLY] ?: false,
                    openOnly = prefs[Keys.FILTER_OPEN_ONLY] ?: false,
                    minStipendDzd = prefs[Keys.FILTER_MIN_STIPEND]?.takeIf { it > 0 },
                    domainId = prefs[Keys.FILTER_DOMAIN_ID]?.takeIf { it > 0L },
                    locationId = prefs[Keys.FILTER_LOCATION_ID]?.takeIf { it > 0L },
                    sortOption = sortOption
                ),
                pinLockEnabled = prefs[Keys.PIN_ENABLED] ?: false,
                pinSalt = prefs[Keys.PIN_SALT] ?: "",
                pinHash = prefs[Keys.PIN_HASH] ?: ""
            )
        }

    suspend fun setLanguage(language: AppLanguage) {
        context.firstStepDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.firstStepDataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = themeMode.key
        }
    }

    suspend fun enablePinLock(pin: String) {
        if (!PinSecurityHelper.isValidPinFormat(pin)) return
        val salt = PinSecurityHelper.generateSalt()
        val hash = PinSecurityHelper.hashPin(pin, salt)
        context.firstStepDataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = true
            prefs[Keys.PIN_SALT] = salt
            prefs[Keys.PIN_HASH] = hash
        }
    }

    suspend fun disablePinLock() {
        context.firstStepDataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = false
            prefs.remove(Keys.PIN_SALT)
            prefs.remove(Keys.PIN_HASH)
        }
    }

    suspend fun saveFilters(criteria: OfferFilterCriteria) {
        context.firstStepDataStore.edit { prefs ->
            prefs[Keys.FILTER_QUERY] = criteria.query
            prefs[Keys.FILTER_PAID_ONLY] = criteria.paidOnly
            prefs[Keys.FILTER_OPEN_ONLY] = criteria.openOnly
            if (criteria.minStipendDzd != null && criteria.minStipendDzd > 0) {
                prefs[Keys.FILTER_MIN_STIPEND] = criteria.minStipendDzd
            } else {
                prefs.remove(Keys.FILTER_MIN_STIPEND)
            }
            if (criteria.domainId != null) {
                prefs[Keys.FILTER_DOMAIN_ID] = criteria.domainId
            } else {
                prefs.remove(Keys.FILTER_DOMAIN_ID)
            }
            if (criteria.locationId != null) {
                prefs[Keys.FILTER_LOCATION_ID] = criteria.locationId
            } else {
                prefs.remove(Keys.FILTER_LOCATION_ID)
            }
            prefs[Keys.FILTER_SORT] = criteria.sortOption.name
        }
    }
}
