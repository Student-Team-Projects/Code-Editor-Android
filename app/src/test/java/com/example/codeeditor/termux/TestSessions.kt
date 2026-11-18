package com.example.codeeditor.termux

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

/**
    A session whose work runs on the test scheduler as ordinary tasks.
    (`backgroundScope` wouldn't do: `advanceUntilIdle()` skips its tasks.)
 */
fun TestScope.testSession() = TermuxSession(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)))
