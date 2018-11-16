package com.graphhopper.util;

import com.graphhopper.util.Helper;
import com.graphhopper.util.Translation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

public class TranslationMap {
   private static final List LOCALES = Arrays.asList(new String[]{"ar", "ast", "bg", "ca", "cs_CZ", "da_DK", "de_DE", "el", "en_US", "es", "fa", "fil", "fi", "fr", "gl", "he", "hsb", "hu_HU", "it", "ja", "lt_LT", "ne", "nl", "pl_PL", "pt_BR", "pt_PT", "ro", "ru", "si", "sk", "sv_SE", "tr", "uk", "vi_VI", "zh_CN"});
   private final Map translations = new HashMap();

   public TranslationMap doImport(File folder) {
      try {
         Iterator ex = LOCALES.iterator();

         while(ex.hasNext()) {
            String locale = (String)ex.next();
            TranslationMap.TranslationHashMap trMap = new TranslationMap.TranslationHashMap(Helper.getLocale(locale));
            trMap.doImport(new FileInputStream(new File(folder, locale + ".txt")));
            this.add(trMap);
         }

         this.postImportHook();
         return this;
      } catch (Exception var5) {
         throw new RuntimeException(var5);
      }
   }

   public TranslationMap doImport() {
      try {
         Iterator ex = LOCALES.iterator();

         while(ex.hasNext()) {
            String locale = (String)ex.next();
            TranslationMap.TranslationHashMap trMap = new TranslationMap.TranslationHashMap(Helper.getLocale(locale));
            trMap.doImport(TranslationMap.class.getResourceAsStream(locale + ".txt"));
            this.add(trMap);
         }

         this.postImportHook();
         return this;
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }

   public void add(Translation tr) {
      Locale locale = tr.getLocale();
      this.translations.put(locale.toString(), tr);
      if(!locale.getCountry().isEmpty() && !this.translations.containsKey(tr.getLanguage())) {
         this.translations.put(tr.getLanguage(), tr);
      }

      if("iw".equals(locale.getLanguage())) {
         this.translations.put("he", tr);
      }

      if("in".equals(locale.getLanguage())) {
         this.translations.put("id", tr);
      }

   }

   public Translation getWithFallBack(Locale locale) {
      Translation tr = this.get(locale.toString());
      if(tr == null) {
         tr = this.get(locale.getLanguage());
         if(tr == null) {
            tr = this.get("en");
         }
      }

      return tr;
   }

   public Translation get(String locale) {
      locale = locale.replace("-", "_");
      Translation tr = (Translation)this.translations.get(locale);
      if(locale.contains("_") && tr == null) {
         tr = (Translation)this.translations.get(locale.substring(0, 2));
      }

      return tr;
   }

   public static int countOccurence(String phrase, String splitter) {
      return Helper.isEmpty(phrase)?0:phrase.trim().split(splitter).length;
   }

   private void postImportHook() {
      Map enMap = this.get("en").asMap();
      StringBuilder sb = new StringBuilder();
      Iterator i$ = this.translations.values().iterator();

      while(i$.hasNext()) {
         Translation tr = (Translation)i$.next();
         Map trMap = tr.asMap();
         Iterator i$1 = enMap.entrySet().iterator();

         while(i$1.hasNext()) {
            Entry enEntry = (Entry)i$1.next();
            String value = (String)trMap.get(enEntry.getKey());
            if(Helper.isEmpty(value)) {
               trMap.put(enEntry.getKey(), enEntry.getValue());
            } else {
               int expectedCount = countOccurence((String)enEntry.getValue(), "\\%");
               if(expectedCount != countOccurence(value, "\\%")) {
                  sb.append(tr.getLocale()).append(" - error in ").append((String)enEntry.getKey()).append("->").append(value).append("\n");
               } else {
                  String[] strs = new String[expectedCount];
                  Arrays.fill(strs, "tmp");

                  try {
                     String.format(value, strs);
                  } catch (Exception var12) {
                     sb.append(tr.getLocale()).append(" - error ").append(var12.getMessage()).append("in ").append((String)enEntry.getKey()).append("->").append(value).append("\n");
                  }
               }
            }
         }
      }

      if(sb.length() > 0) {
         System.out.println(sb);
         throw new IllegalStateException(sb.toString());
      }
   }

   public String toString() {
      return this.translations.toString();
   }

   public static class TranslationHashMap implements Translation {
      private final Map map = new HashMap();
      final Locale locale;

      public TranslationHashMap(Locale locale) {
         this.locale = locale;
      }

      public void clear() {
         this.map.clear();
      }

      public Locale getLocale() {
         return this.locale;
      }

      public String getLanguage() {
         return this.locale.getLanguage();
      }

      public String tr(String key, Object... params) {
         String val = (String)this.map.get(key.toLowerCase());
         return Helper.isEmpty(val)?key:String.format(val, params);
      }

      public TranslationMap.TranslationHashMap put(String key, String val) {
         String existing = (String)this.map.put(key.toLowerCase(), val);
         if(existing != null) {
            throw new IllegalStateException("Cannot overwrite key " + key + " with " + val + ", was: " + existing);
         } else {
            return this;
         }
      }

      public String toString() {
         return this.map.toString();
      }

      public Map asMap() {
         return this.map;
      }

      public TranslationMap.TranslationHashMap doImport(InputStream is) {
         if(is == null) {
            throw new IllegalStateException("No input stream found in class path!?");
         } else {
            try {
               Iterator ex = Helper.readFile((Reader)(new InputStreamReader(is, Helper.UTF_CS))).iterator();

               while(ex.hasNext()) {
                  String line = (String)ex.next();
                  if(!line.isEmpty() && !line.startsWith("//") && !line.startsWith("#")) {
                     int index = line.indexOf(61);
                     if(index >= 0) {
                        String key = line.substring(0, index);
                        if(key.isEmpty()) {
                           throw new IllegalStateException("No key provided:" + line);
                        }

                        String value = line.substring(index + 1);
                        if(!value.isEmpty()) {
                           this.put(key, value);
                        }
                     }
                  }
               }

               return this;
            } catch (IOException var7) {
               throw new RuntimeException(var7);
            }
         }
      }
   }
}
