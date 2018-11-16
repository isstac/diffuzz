package com.graphhopper.util;

import java.util.Locale;
import java.util.Map;

public interface Translation {
   String tr(String var1, Object... var2);

   Map asMap();

   Locale getLocale();

   String getLanguage();
}
