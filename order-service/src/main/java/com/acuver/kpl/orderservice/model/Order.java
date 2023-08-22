package com.acuver.kpl.orderservice.model;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("order")
public class Order {

//    private String productId;
//    private Integer quantity;

    private String receiverName;
    private String address;
    private String city;
    private String state;
    private String zipcode;
    private String country;
    private String mobile;
    private String email;
    private String paymentMethod;
    @Id
    private String sellerOrderId;   // uniquely identify order
    @CreatedDate
    private LocalDateTime createdAt = LocalDateTime.now();
//    private Date lastModifiedOn;
    private Integer shippingCharge;
    //    private Date orderDate;
    private String sellerId;
    private String status;
    private String store;
    private List<OrderLine> orderLineEntries = new ArrayList<>();

    @Getter
    @Setter
    public static class OrderLine {
        private Boolean onHold;
        private String orderLineId;
        private String sku;   // productId
        //        private Date invoiceDate;
        private Integer lineFinalAmount;
        private String sellerOrderId;
        private String invoiceNumber;
        private Integer quantity;    // quantity
        //        private Date createdOn;
        private String orderId;
        private Integer cancelledQuantity;
        private String status;  // status
        private String cancellationReason;
//        private String shippingMethod;
    }
}
