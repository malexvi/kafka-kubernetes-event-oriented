package com.sfr.sfr_orquestrador_api.domain;

public enum DeliveryStatus {
    INICIADO(1),
    PENDENTE_TIPO_ENTREGA(2),
    TIPO_ENTREGA_DEFINIDO(3),
    FINALIZADO(4);

    private int code;


    private DeliveryStatus(int code) { 
        this.code = code;
    }


    public int getCode() {
        return code;
    }


    public void setCode(int code) {
        this.code = code;
    }

    public static DeliveryStatus valueOf(int code) {
        for (DeliveryStatus value : DeliveryStatus.values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid code");
    }
}