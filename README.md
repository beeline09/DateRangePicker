# DateRangePicker
[ ![Download](https://api.bintray.com/packages/beeline09/daterangepicker/DateRangePicker/images/download.svg?version=1.0.2) ](https://bintray.com/beeline09/daterangepicker/DateRangePicker/1.0.2/link)

Based on [SmoothDateRangePicker by leavjenn] (https://github.com/leavjenn/SmoothDateRangePicker) and migrated to Kotlin and androidX

Smooth Date Range Picker is an android widget for selecting date range quickly and easily, following Material Design principle. This library is based on [datetimepicker from Android frameworks](https://android.googlesource.com/platform/frameworks/opt/datetimepicker/) and [Material DateTime Picker by wdullaer](https://github.com/wdullaer/MaterialDateTimePicker).

Date Range Picker | Duration Number Pad | Date Range Picker Dark Theme | Date Range Picker Landscape
---- | ----| ----| ----
![Date Range Picker](https://raw.githubusercontent.com/leavjenn/SmoothDateRangePicker/gh-pages/screenshots/date_range_picker.png) | ![Duration Number Pad](https://raw.githubusercontent.com/leavjenn/SmoothDateRangePicker/gh-pages/screenshots/date_duration_number_pad.png) | ![Date Range Picker Dark Theme](https://raw.githubusercontent.com/leavjenn/SmoothDateRangePicker/gh-pages/screenshots/date_range_picker_dark_theme.png) | ![Date Range Picker Landscape](https://raw.githubusercontent.com/leavjenn/SmoothDateRangePicker/gh-pages/screenshots/date_range_picker_landscape.png)

## Setup

**Gradle:**

Add jcenter repository into project `build.gradle`:
```
repositories {
    jcenter()
}
```

Add the following into app `build.gradle`:
```
dependencies {
  implementation 'com.github.beeline09:DateRangePicker:1.0.2'
}
```

## Usage

Since DateRangePickerFragment is a subclass of DialogFragment, the usage is just like other DialogFragment.

**Instantiation, the default selected date is set to today:**
```
DateRangePickerFragment.newInstance(DateRangePickerFragment.OnDateRangeSetListener callBack)
```


**Instantiation, specify the default selected date:**
```
DateRangePickerFragment.newInstance(DateRangePickerFragment.OnDateRangeSetListener callBack, 
int year, int monthOfYear, int dayOfMonth)
```

After instantiation, remember to call

`show (SupportFragmentManager manager, String tag)`

Example:
```
DateRangePickerFragment dateRangePickerFragment = DateRangePickerFragment.newInstance(
new DateRangePickerFragment.OnDateRangeSetListener() {
                    @Override
                    public void onDateRangeSet(DateRangePickerFragment view,
                                               int yearStart, int monthStart,
                                               int dayStart, int yearEnd,
                                               int monthEnd, int dayEnd) {
                        // grab the date range, do what you want
                    }
                });
                
dateRangePickerFragment.show(getSupportFragmentManager(), "dateRangePicker");
```


## Additional options

### Style
**Change picker accent color:**

`setAccentColor(int color)`

**Change picker theme to dark:**

`setThemeDark(true)`


### Date
**Set selectable minimal date:**

`setMinDate(Calendar calendar)`

**Set selectable max date:**

`setMaxDate(Calendar calendar)`


## License
    Copyright (c) 2015 Leav Jenn

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
