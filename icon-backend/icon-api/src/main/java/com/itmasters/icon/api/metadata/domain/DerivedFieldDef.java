package com.itmasters.icon.api.metadata.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DerivedFieldDef {
    private String name;
    private String label;
    private String category; // free text or enum name
    private String description;
    private List<String> availableOperators; // operator enum names
    private List<ValueOption> valueOptions;

    @Getter
    @Setter
    public static class ValueOption {
        private String value;
        private String label;
    }
}

