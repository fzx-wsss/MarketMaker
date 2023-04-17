package com.wsss.market.maker.model.center;


import com.wsss.market.maker.model.config.BiAnConfig;

public class ConfigCenter {
    private static BiAnConfig biAnConfig;

    public static BiAnConfig getApolloConfig() {
        return biAnConfig;
    }

    public static void setApolloConfig(BiAnConfig biAnConfig) {
        ConfigCenter.biAnConfig = biAnConfig;
    }
}
