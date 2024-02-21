package com.chilun.apiopenspace.gateway.model.pojo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 接口申请实体
 *
 * @TableName interface_access
 */
@Data
public class InterfaceAccess implements Serializable {
    private String accesskey;
    private Integer verifyType;
    private String secretkey;
    private BigDecimal remainingAmount;
    private Integer remainingTimes;
    private Long interfaceId;
    private Long userid;
    private Date createTime;
    private Date updateTime;
    private Integer isDeleted;
}