package com.josie.tripstar.entity.vo;

import lombok.Data;
import javax.validation.constraints.*;

/** 用户手动维护的显式偏好；权重由服务端设置。 */
@Data
public class MemoryRequest {
    @NotBlank @Size(max = 120) private String content;
}
