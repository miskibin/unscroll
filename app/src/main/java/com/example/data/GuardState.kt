package com.example.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BudgetSnapshot(
    val usedMs: Long = 0,
    val allowanceMs: Long = 120_000,
    val windowMs: Long = 600_000,
    val resetInMs: Long = 0,
    val exhausted: Boolean = false,
    val updatedAt: Long = 0,
)

/**
 * In-process bridge between the accessibility service and the dashboard:
 * the service publishes its live budget here, the UI just collects it.
 */
object GuardState {
    private val _serviceRunning = MutableStateFlow(false)
    val serviceRunning: StateFlow<Boolean> = _serviceRunning

    private val _budget = MutableStateFlow(BudgetSnapshot())
    val budget: StateFlow<BudgetSnapshot> = _budget

    fun publishServiceRunning(running: Boolean) {
        _serviceRunning.value = running
    }

    fun publishBudget(snapshot: BudgetSnapshot) {
        _budget.value = snapshot
    }
}
