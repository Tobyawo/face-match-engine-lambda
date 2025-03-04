package com.gm.facematch.engine.data;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;



@Getter
@Setter
public class MatchRequestDto {
    @NotBlank(message = "image1Url is mandatory")
    private String image1Url;

    @NotBlank(message = "image2Url is mandatory")
    private String image2Url;

//    @Pattern(regexp = "^\\w{5,15}-\\d{14}$", message = "TransactionId should be in the format: MerchantId-yyyyMMddhhmmss. MerchantId should be 5 to 15 characters")
    private String transactionId;

    private String callBackUrl;

    private String userName;

}
