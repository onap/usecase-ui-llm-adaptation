/*
 * Copyright (C) 2024 CMCC, Inc. and others. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onap.usecaseui.llmadaptation.util;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TimeUtil {

    public static final String timeFormat = "yyyy-MM-dd HH:mm:ss";

    public static String getFormattedDateTime() {
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(timeFormat);
        String formattedDateTime = now.format(dateTimeFormatter);

        DayOfWeek dayOfWeek = now.getDayOfWeek();
        String dayOfWeekString = dayOfWeek.toString();
        return formattedDateTime + " " + dayOfWeekString.charAt(0) + dayOfWeekString.substring(1).toLowerCase();
    }

    public static String getNowTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(timeFormat);
        return now.format(dateTimeFormatter);
    }
}
