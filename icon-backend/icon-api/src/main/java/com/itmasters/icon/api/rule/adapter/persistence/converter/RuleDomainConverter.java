package com.itmasters.icon.api.rule.adapter.persistence.converter;

import com.itmasters.icon.common.domain.RuleDomain;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Converter(autoApply = false)
public class RuleDomainConverter implements AttributeConverter<RuleDomain, String> {

    private static final Map<String, RuleDomain> ALIASES = new HashMap<>();
    static {
        // canonical names
        ALIASES.put("LOGIN", RuleDomain.LOGIN);
        ALIASES.put("ATM", RuleDomain.ATM);
        ALIASES.put("FINANCIAL_TRANSACTION", RuleDomain.FINANCIAL_TRANSACTION);
        ALIASES.put("DEVICE_SECURITY", RuleDomain.DEVICE_SECURITY);
        ALIASES.put("CUSTOMER", RuleDomain.CUSTOMER);
        ALIASES.put("ACCOUNT", RuleDomain.ACCOUNT);
        ALIASES.put("FREQUENCY", RuleDomain.FREQUENCY);

        // legacy/short aliases
        ALIASES.put("AUTH", RuleDomain.LOGIN);
        ALIASES.put("AUTHENTICATION", RuleDomain.LOGIN);
        ALIASES.put("SECURITY_LOGIN", RuleDomain.LOGIN);

        ALIASES.put("FINANCIAL", RuleDomain.FINANCIAL_TRANSACTION);
        ALIASES.put("TRANSACTION", RuleDomain.FINANCIAL_TRANSACTION);
        ALIASES.put("FINANCE", RuleDomain.FINANCIAL_TRANSACTION);

        ALIASES.put("DEVICE", RuleDomain.DEVICE_SECURITY);
        ALIASES.put("DEVICE_SEC", RuleDomain.DEVICE_SECURITY);

        ALIASES.put("ACCT", RuleDomain.ACCOUNT);
        ALIASES.put("CUST", RuleDomain.CUSTOMER);
    }

    @Override
    public String convertToDatabaseColumn(RuleDomain attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RuleDomain convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) return null;
        String key = dbData.trim().toUpperCase(Locale.ROOT);
        // normalize hyphen/space to underscore
        key = key.replace('-', '_').replace(' ', '_');

        RuleDomain mapped = ALIASES.get(key);
        if (mapped != null) return mapped;

        try {
            return RuleDomain.valueOf(key);
        } catch (IllegalArgumentException ex) {
            // unknown legacy value → treat as null to avoid boot failure
            return null;
        }
    }
}

