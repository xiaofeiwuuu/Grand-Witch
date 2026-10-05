package com.xiaofeiwu.grandwitch;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every page of the diary is in both languages, and fits on a page of a book (a page holds about 14 lines of 19 characters in Chinese, more in English). */
class DiaryLangTest {

    @Test
    void everyPageIsThereInBothLanguagesAndIsNotTooLongForAPage() throws Exception {
        for (String language : new String[]{"zh_cn", "en_us"}) {
            String json = Files.readString(Path.of("src/main/resources/assets/grandwitch/lang/" + language + ".json"), StandardCharsets.UTF_8);
            for (int i = 1; i <= WitchDiary.PAGES; i++) {
                Matcher m = Pattern.compile("\"book\\.grandwitch\\.diary\\." + i + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(json);
                assertTrue(m.find(), language + " lacks page " + i);
                int limit = language.equals("zh_cn") ? 190 : 330;
                assertTrue(m.group(1).length() <= limit, language + " page " + i + " is " + m.group(1).length() + " characters, over " + limit);
            }
        }
    }
}
