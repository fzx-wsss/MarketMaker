package com.wsss.market.maker.model.domain;

import com.wsss.market.maker.model.domain.maker.Operation;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderCommand {
    private Order order;
    private Operation operation;
}
