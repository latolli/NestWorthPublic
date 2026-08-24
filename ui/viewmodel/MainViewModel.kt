package com.example.nestworth.ui.viewmodel

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestworth.Repository.db.AppDatabase
import com.example.nestworth.Repository.model.Profile
import com.example.nestworth.Repository.model.Asset
import com.example.nestworth.Repository.model.AssetDatapoint
import com.example.nestworth.Repository.model.AssetWithDatapoints
import com.example.nestworth.Repository.model.Expense
import com.example.nestworth.Repository.model.ExpenseCategory
import com.example.nestworth.Repository.settings.SettingsRepository
import com.example.nestworth.achievement.AchievementEvaluator
import com.example.nestworth.achievement.StartingStep
import com.example.nestworth.core.Constants.XP_PER_LEVEL
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class MainViewModel(private val db: AppDatabase, private val settingsRepository: SettingsRepository) : ViewModel() {

    // Expenses
    fun addExpense(profile: Profile, amount: Double, category: String, note: String) {
        viewModelScope.launch {
            db.expenseDao().insertExpense(
                Expense(amount = amount, category = category, note = note)
            )
            checkAchievements(profile)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            db.expenseDao().deleteExpense(expense)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateExpense(profile: Profile, expense: Expense, amount: Double, category: String, note: String, date: LocalDate) {
        viewModelScope.launch {
            val epochMillis = date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            db.expenseDao().updateExpense(
                expense.copy(amount = amount, category = category, note = note, date = epochMillis)
            )
            checkAchievements(profile)
        }
    }

    val allExpenses = db.expenseDao().getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Incomes
    fun addIncome(profile: Profile, amount: Double, note: String) {
        viewModelScope.launch {
            db.expenseDao().insertExpense(
                Expense(amount = amount, category="", note = note, isIncome = true)
            )
            checkAchievements(profile)
        }
    }

    // Assets
    // Now takes the profile directly and checks achievements *after* the write
    // has actually committed, instead of leaving the caller to read totalNetWorth.value
    // immediately afterward (which can still be stale due to StateFlow propagation lag).
    // checkAchievements itself pulls a fresh net worth and event count straight from
    // the DB, so callers no longer need to compute them beforehand.
    fun addAssetWithDatapoint(profile: Profile, name: String, type: String, value: Double, liability: Double) {
        viewModelScope.launch {
            val assetId = db.assetDao().insertAsset(
                Asset(name = name, type = type)
            )
            db.assetDatapointDao().insertDatapoint(
                AssetDatapoint(assetId = assetId.toInt(), value = value, liability = liability)
            )
            checkAchievements(profile)
        }
    }


    fun updateAsset(asset: Asset, name: String, type: String) {
        viewModelScope.launch {
            val updatedAsset = asset.copy(name = name, type = type)
            db.assetDao().updateAsset(updatedAsset)
        }
    }
    fun deleteAsset(asset: Asset) {
        viewModelScope.launch {
            db.assetDao().deleteAsset(asset)
        }
    }

    val allAssets = db.assetDao().getAllAssets()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val allAssetsWithDatapoints = db.assetDao().getAllAssetsWithDatapoints()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Total net worth derived from latest datapoint of each asset
    val totalNetWorth: StateFlow<Double> = allAssetsWithDatapoints
        .map { list -> computeNetWorth(list) }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // Reads the current asset list straight from the DB flow (bypassing the
    // StateFlow cache) and computes net worth from it. Safe to call right after
    // a suspend insert completes, since Room's invalidation has already fired
    // by the time the insert's coroutine resumes.
    private suspend fun computeNetWorth(): Double =
        computeNetWorth(db.assetDao().getAllAssetsWithDatapoints().first())

    private fun computeNetWorth(list: List<AssetWithDatapoints>): Double =
        list.sumOf { assetWithDatapoints ->
            val latest = assetWithDatapoints.datapoints.maxByOrNull { it.date }
            (latest?.value ?: 0.0) - (latest?.liability ?: 0.0)
        }

    // Same idea as computeNetWorth(): reads straight from the DB flow instead of
    // the (potentially stale) allExpenses StateFlow, so it's safe to call right
    // after a suspend insert/update completes.
    private suspend fun computeLoggedEventCount(): Int =
        db.expenseDao().getAllExpenses().first().size

    private fun computeNWGrowth(
        assets: List<AssetWithDatapoints>,
        cutoffTime: Long
    ): Double {
        val currentNW = assets.sumOf { asset ->
            val latest = asset.datapoints.maxByOrNull { it.date }
            (latest?.value ?: 0.0) - (latest?.liability ?: 0.0)
        }
        val pastNW = assets.sumOf { asset ->
            val pastLatest = asset.datapoints
                .filter { it.date <= cutoffTime }
                .maxByOrNull { it.date }
            (pastLatest?.value ?: 0.0) - (pastLatest?.liability ?: 0.0)
        }

        return currentNW - pastNW
    }

    val netWorthGrowthLast30Days: StateFlow<Double> =
        allAssetsWithDatapoints
            .map { list ->
                val thirtyDaysAgo =
                    System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

                computeNWGrowth(
                    assets = list,
                    cutoffTime = thirtyDaysAgo
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.Lazily,
                0.0
            )

    val timeRangeNWGrowth: StateFlow<Double> =
        combine(
            allAssetsWithDatapoints,
            settingsRepository.settings
        ) { list, settings ->

            val cutoffTime =
                settings.timeRange.cutoffTime(System.currentTimeMillis())

            computeNWGrowth(
                assets = list,
                cutoffTime = cutoffTime
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0.0
        )

    // Calculate savings rate
    val incomeLast30Days: StateFlow<Double> = allExpenses
        .map { list ->
            list
                .filter { it.date >= System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) }
                .filter { it.isIncome }
                .sumOf { income -> income.amount }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val expensesLast30Days: StateFlow<Double> = allExpenses
        .map { list ->
            list
                .filter { it.date >= System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000) }
                .filter { !it.isIncome }
                .sumOf { expense -> expense.amount }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val savingsRateLast30Days: StateFlow<Double> = combine(
        incomeLast30Days,
        expensesLast30Days
    ) { income, expenses ->
        if (income > 0.0) ((income - expenses) / income) * 100.0 else 0.0
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // --- 45-day backup window ---
    val incomeLast45Days: StateFlow<Double> = allExpenses
        .map { list ->
            list
                .filter { it.date >= System.currentTimeMillis() - (45L * 24 * 60 * 60 * 1000) }
                .filter { it.isIncome }
                .sumOf { income -> income.amount }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val expensesLast45Days: StateFlow<Double> = allExpenses
        .map { list ->
            list
                .filter { it.date >= System.currentTimeMillis() - (45L * 24 * 60 * 60 * 1000) }
                .filter { !it.isIncome }
                .sumOf { expense -> expense.amount }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val savingsRateLast45Days: StateFlow<Double?> = combine(
        incomeLast45Days,
        expensesLast45Days
    ) { income, expenses ->
        when {
            income > 0.0 -> ((income - expenses) / income) * 100.0
            expenses > 0.0 -> 0.0
            else -> 0.0
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // --- Combined value: use 30-day, fall back to 45-day if no 30-day income ---
    data class SavingsRateResult(
        val rate: Double?,
        val isFallback: Boolean // true if this came from the 45-day window
    )

    val displaySavingsRate: StateFlow<SavingsRateResult> = combine(
        incomeLast30Days,
        savingsRateLast30Days,
        savingsRateLast45Days
    ) { income30, rate30, rate45 ->
        if (income30 > 0.0) {
            SavingsRateResult(rate = rate30, isFallback = false)
        } else {
            SavingsRateResult(rate = rate45, isFallback = true)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        SavingsRateResult(rate = 0.0, isFallback = false)
    )

    val netWorthGrowthPercent: StateFlow<Double?> = combine(
        totalNetWorth,
        netWorthGrowthLast30Days
    ) { current, growth ->
        val past = current - growth
        if (past != 0.0) (growth / past) * 100 else null
    }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val highestEquityAsset: StateFlow<Pair<String, Double>?> = allAssetsWithDatapoints
        .map { assets ->
            assets
                .mapNotNull { asset ->
                    asset.datapoints
                        .maxByOrNull { it.date }
                        ?.let { latest -> asset.asset.name to (latest.value - latest.liability) }
                }
                .maxByOrNull { it.second }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val totalAssetValue: StateFlow<Double> = allAssetsWithDatapoints
        .map { list ->
            list.sumOf { assetWithDatapoints ->
                assetWithDatapoints.datapoints.maxByOrNull { it.date }?.value ?: 0.0
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    val totalLiability: StateFlow<Double> = allAssetsWithDatapoints
        .map { list ->
            list.sumOf { assetWithDatapoints ->
                assetWithDatapoints.datapoints.maxByOrNull { it.date }?.liability ?: 0.0
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    // Asset datapoints
    // Also updated to take the profile and check achievements after the write commits.
    @RequiresApi(Build.VERSION_CODES.O)
    fun addDatapoint(profile: Profile, asset: Asset, value: Double, liability: Double, date: LocalDate) {
        viewModelScope.launch {
            val epochMillis = date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            db.assetDatapointDao().insertDatapoint(
                AssetDatapoint(assetId = asset.id, value = value, liability = liability, date = epochMillis)
            )
            checkAchievements(profile)
        }
    }

    fun deleteDatapoint(datapoint: AssetDatapoint) {
        viewModelScope.launch {
            db.assetDatapointDao().deleteDatapoint(datapoint)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun updateDatapoint(profile: Profile, datapoint: AssetDatapoint, value: Double, liability: Double, date: LocalDate) {
        viewModelScope.launch {
            val epochMillis = date
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            db.assetDatapointDao().updateDatapoint(
                datapoint.copy(value = value, liability = liability, date = epochMillis)
            )
            checkAchievements(profile)
        }
    }

    fun getDatapointsForAsset(assetId: Int): StateFlow<List<AssetDatapoint>> =
        db.assetDatapointDao().getDatapointsForAsset(assetId)
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun getAssetWithDatapoints(assetId: Int): StateFlow<AssetWithDatapoints?> =
        allAssetsWithDatapoints
            .map { list -> list.find { it.asset.id == assetId } }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Expense Categories
    fun addExpenseCategory(name: String, emoji: String) {
        viewModelScope.launch {
            db.expenseCategoryDao().insertExpenseCategory(
                ExpenseCategory(name = name, emoji = emoji)
            )
        }
    }

    fun updateExpenseCategory(category: ExpenseCategory, name: String, emoji: String) {
        viewModelScope.launch {
            val updatedCategory = category.copy(name = name, emoji = emoji)
            db.expenseCategoryDao().updateExpenseCategory(updatedCategory)
        }
    }

    fun deleteExpenseCategory(category: ExpenseCategory) {
        viewModelScope.launch {
            db.expenseCategoryDao().deleteExpenseCategory(category)
        }
    }

    val allExpenseCategories = db.expenseCategoryDao().getAllExpenseCategories()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Profile
    fun addProfile(name: String) {
        viewModelScope.launch {
            db.profileDao().insertProfile(
                Profile(name = name,
                    xpAmount = 0,
                    xpLevel = 0,
                    achievements = List(1) { 1 },
                    startingSteps = StartingStep.PROFILE_CREATED.mask)  // Auto unlock first achievement
            )

            // Add default expense categories
            db.expenseCategoryDao().insertAll(
                listOf(
                    ExpenseCategory(name = "Entertainment", emoji = "🎵", isDefault = true),
                    ExpenseCategory(name = "Bills", emoji = "🧾", isDefault = true),
                    ExpenseCategory(name = "Housing", emoji = "🏠", isDefault = true),
                    ExpenseCategory(name = "Transport", emoji = "🚗", isDefault = true),
                    ExpenseCategory(name = "Groceries", emoji = "🛒", isDefault = true),
                )
            )
        }
    }

    // Delete profile and ALL user data
    fun deleteProfile(profile: Profile) {
        viewModelScope.launch {
            db.profileDao().deleteProfile(profile)
            db.clearAllUserData()
        }
    }

    fun updateProfile(profile: Profile, name: String, xpAmount: Int, xpLevel: Int,
                      achievements: List<Int>, streak: Int, lastLogin: Long,
                      imageUri: String? = profile.imageUri, startingSteps: Int? = profile.startingSteps){
        viewModelScope.launch {
            // Check level up
            // Never reset XP amount, just display it correctly
            val newLevel = xpAmount / XP_PER_LEVEL
            val updatedProfile = profile.copy(name = name, xpAmount = xpAmount,
                xpLevel = newLevel, achievements = achievements, dailyStreak = streak,
                lastLogin = lastLogin, imageUri = imageUri, startingSteps = startingSteps ?: profile.startingSteps
            )
            db.profileDao().updateProfile(updatedProfile)

            // Check achievements if needed
            if ((newLevel > profile.xpLevel) || (streak != profile.dailyStreak) || (startingSteps != profile.startingSteps)) {
                checkAchievements(updatedProfile)
            }
        }
    }

    val allProfiles: StateFlow<List<Profile>?> = db.profileDao().getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // For now, support only 1 profile and always use latest from database
    val latestProfile: StateFlow<Profile?> = allProfiles
        .map { profiles ->
            profiles?.maxByOrNull { it.creationDate }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    // Achievements
    private val _achievementUnlockedEvent = MutableStateFlow<List<Int>?>(null)
    val achievementUnlockedEvent: StateFlow<List<Int>?> = _achievementUnlockedEvent

    // Pulls a fresh net worth and logged-event count straight from the DB
    // (bypassing the StateFlow caches, which may not have propagated yet) and
    // evaluates achievements against them. Callers just need to have committed
    // their write beforehand and be inside a coroutine.
    private suspend fun checkAchievements(profile: Profile) {
        val freshNetWorth = computeNetWorth()
        val freshLoggedEventCount = computeLoggedEventCount()
        val result = AchievementEvaluator.evaluateAchievements(profile, freshNetWorth, freshLoggedEventCount)
        if (result.newIds.isNotEmpty()) {
            updateProfile(profile, profile.name, result.updatedProfile.xpAmount, result.updatedProfile.xpLevel,
                result.updatedProfile.achievements, profile.dailyStreak, profile.lastLogin)
            _achievementUnlockedEvent.value = (_achievementUnlockedEvent.value ?: emptyList()) + result.newIds

        }
    }

    fun consumeAchievementEvent() {
        _achievementUnlockedEvent.value = null
    }
}