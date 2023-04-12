package com.wsss.market.maker.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UserBBO {
    private BigDecimal userBuyPrice;
    private BigDecimal userBuyVolume;
    private BigDecimal userSellPrice;
    private BigDecimal userSellVolume;
}
