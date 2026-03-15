package com.chenxiaofei.disruptflow.support.mq.model;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
public enum DelayLevel {



    NO(1,"NO DELAY"),
    D1(2,"1s"),
    D2(3,"5s"),
    D3(4,"10s"),
    D4(5,"30s"),
    D5(6,"1m"),
    D6(7,"2m"),
    D7(8,"3m"),
    D8(9,"4m"),
    D9(10,"5m"),
    D10(11,"6m"),
    D11(12,"7m"),
    D12(13,"8m"),
    D13(14,"9m"),
    D14(15,"10m"),
    D15(16,"20m"),
    D16(17,"30m"),
    D17(18,"1h"),
    D18(19,"2h"),
    ;
    public int level;
    public String desc;

    DelayLevel(int level, String desc){
        this.level = level;
        this.desc = desc;
    }

    public static DelayLevel getLevel(int level){
        for(DelayLevel value : values()){
            if(value.level == level){
                return value;
            }
        }
        return NO;
    }

}
