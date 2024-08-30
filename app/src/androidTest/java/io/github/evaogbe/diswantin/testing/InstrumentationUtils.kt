package io.github.evaogbe.diswantin.testing

import androidx.annotation.StringRes
import androidx.test.platform.app.InstrumentationRegistry

fun stringResource(@StringRes resId: Int): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(resId)
