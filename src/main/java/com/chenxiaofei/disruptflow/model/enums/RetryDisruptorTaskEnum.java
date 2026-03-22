package com.chenxiaofei.disruptflow.model.enums;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: 任务枚举
 */
@Getter
public enum RetryDisruptorTaskEnum {

    DEMO_TASK(1,"demo任务1","demoTaskProcessor1"),
    DEMO_TASK2(2,"demo任务2","demoTaskProcessor2"),
    CANCEL_EXPRESS(3,"运单取消","orderCancelExpress"),
    CANCEL_DELIVER(4,"配送单取消","orderCancelDeliver"),
    CANCEL_SHOP_INVENTORY(5,"门店库存取消","orderCancelShopInventory"),
    CANCEL_WAREHOUSE_INVENTORY(6,"取消归还仓库库存","orderCancelWarehouseInventory"),
    CANCEL_REFUND(7,"取消生成退款单","orderCancelGenerateRefund"),
    CANCEL_ORDER_DELIVER_EXCEPTION(8,"取消异常发货单类型","orderCancelProcessOrderException"),
    CANCEL_GROUP_ORDER(9,"取消拼团订单","orderCancelGroupOrder"),
    CANCEL_ORDER_AFTERSALE_TASK(10,"取消订单创建售后单退款","orderCancelWechatLiveRefundTask"),
    CANCEL_FREE_GOODS_COUPON(11,"取消赠品优惠券","orderCancelReturnFreeGoodActCouponTask"),
    ;


    private Integer value;

    private String desc;

    private String beanName;

    RetryDisruptorTaskEnum(Integer value, String desc, String beanName){
        this.value = value;
        this.desc = desc;
        this.beanName = beanName;
    }
    public static Optional<RetryDisruptorTaskEnum>findTaskProcessor(String beanName){
        return Stream.of(values())
                .filter(t ->  StringUtils.equals(t.getBeanName(),beanName))
                .findFirst();
    }
}
