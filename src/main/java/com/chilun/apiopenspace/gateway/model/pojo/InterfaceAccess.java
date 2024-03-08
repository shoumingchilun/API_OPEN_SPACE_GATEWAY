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
    private BigDecimal cost;
    private Date expiration;
    private Long interfaceId;
    private Long userid;
    private Integer callTimes;
    private Integer failedCallTimes;
    private Date createTime;
    private Date updateTime;
    private Integer isDeleted;
    private static final long serialVersionUID = 1L;
}