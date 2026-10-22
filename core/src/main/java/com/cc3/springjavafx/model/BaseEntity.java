package com.cc3.springjavafx.model;

import com.cc3.springjavafx.model.dto.BaseDto;

import java.io.Serializable;

public interface BaseEntity extends Serializable {

    BaseDto buildDto();
}
