/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.beeline09.daterangepicker.date

import android.content.Context

/**
 * An adapter for a list of [SimpleMonthView] items.
 */
class SimpleMonthAdapter(context: Context, controller: DateRangePickerController?) : MonthAdapter(context, controller) {

    override fun createMonthView(context: Context): MonthView {
        return SimpleMonthView(context, null, mController)
    }
}
