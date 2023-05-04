package com.wsss.market.maker.service.center;


import com.wsss.market.maker.model.config.SourceConfig;

public class ConfigCenter {
    private static SourceConfig sourceConfig;

    public static SourceConfig getApolloConfig() {
        return sourceConfig;
    }

    public static void setApolloConfig(SourceConfig sourceConfig) {
        ConfigCenter.sourceConfig = sourceConfig;
    }
}
