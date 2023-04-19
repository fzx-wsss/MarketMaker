package com.wsss.market.maker.model.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {
    private Long id;
    private BigDecimal price;
    private BigDecimal volume;
    private Long bidId;
    private Long askId;
    private Side trendSide;
    private Integer bidUserId;
    private Integer askUserId;
    private BigDecimal buyFee;
    private BigDecimal sellFee;
    private String buyFeeCoin;
    private String sellFeeCoin;
    private Date ctime;
    private Date mtime;
    private String symbol;
}