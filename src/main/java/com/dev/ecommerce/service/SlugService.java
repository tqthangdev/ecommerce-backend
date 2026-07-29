package com.dev.ecommerce.service;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class SlugService {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern DASHES = Pattern.compile("-+");

    public String slugify(String input) {
        if (input == null) return "";
        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String stripped = Normalizer.normalize(normalized, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String lower = stripped.toLowerCase(Locale.ENGLISH);
        String cleaned = NON_LATIN.matcher(lower).replaceAll("");
        return DASHES.matcher(cleaned).replaceAll("-").replaceAll("^-|-$", "");
    }
}