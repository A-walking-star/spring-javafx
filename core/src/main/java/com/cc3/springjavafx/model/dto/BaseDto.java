package com.cc3.springjavafx.model.dto;

import com.cc3.springjavafx.util.ParamsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public interface BaseDto extends Serializable {
    Logger log = LoggerFactory.getLogger(BaseDto.class);

    /**
     * json 字符串化
     * @return json字符串
     */
    default String stringify() {
        try {
            return ParamsUtils.serialize(this);
        } catch (Throwable e) {
            log.error("serialize dto to json failed", e);
        }
        return "";
    };
}
