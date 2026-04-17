package com.itmasters.icon.api.common.support;

import org.junit.jupiter.api.BeforeEach;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.MatchingStrategies;

public abstract class MapperTestSupport {

  protected ModelMapper modelMapper;

  @BeforeEach
  protected void setUp() {
    modelMapper = new ModelMapper();
    modelMapper
        .getConfiguration()
        .setFieldMatchingEnabled(true)
        .setFieldAccessLevel(AccessLevel.PRIVATE)
        .setMatchingStrategy(MatchingStrategies.STRICT);
  }
}
