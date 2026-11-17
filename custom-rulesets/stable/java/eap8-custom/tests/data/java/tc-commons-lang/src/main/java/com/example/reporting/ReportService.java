package com.example.reporting;

import org.apache.commons.lang.StringUtils;

/**
 * Test source for rule eap8-custom-00020 (commons-lang 2.x dependency).
 */
public class ReportService {

    public String formatTitle(String title) {
        return StringUtils.capitalize(title);
    }
}
