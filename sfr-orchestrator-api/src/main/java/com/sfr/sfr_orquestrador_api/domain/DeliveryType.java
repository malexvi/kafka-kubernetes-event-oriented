package com.sfr.sfr_orquestrador_api.domain;

public enum DeliveryType {
    AEREO(1),
    RODOVIARIO(2),
    LOCAL(3);

    private int code;


    private DeliveryType(int code) { 
        this.code = code;
    }


    public int getCode() {
        return code;
    }


    public void setCode(int code) {
        this.code = code;
    }

    public static DeliveryType valueOf(int code) {
        for (DeliveryType value : DeliveryType.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid code");
    }
}
