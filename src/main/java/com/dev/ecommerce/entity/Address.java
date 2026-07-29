package com.dev.ecommerce.entity;

import com.dev.ecommerce.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "addresses")
public class Address extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String recipientName;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "province_code", nullable = false, length = 20)
    private String provinceCode;

    @Column(name = "province_name", nullable = false, length = 100)
    private String provinceName;

    @Column(name = "district_code", nullable = false, length = 20)
    private String districtCode;

    @Column(name = "district_name", nullable = false, length = 100)
    private String districtName;

    @Column(name = "ward_code", nullable = false, length = 20)
    private String wardCode;

    @Column(name = "ward_name", nullable = false, length = 100)
    private String wardName;

    @Column(nullable = false, length = 500)
    private String streetAddress;

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress = false;

    @Column(length = 50)
    private String label;

    public Address(User user, String recipientName, String phone,
                   String provinceCode, String provinceName,
                   String districtCode, String districtName,
                   String wardCode, String wardName, String streetAddress) {
        this.user = user;
        this.recipientName = recipientName;
        this.phone = phone;
        this.provinceCode = provinceCode;
        this.provinceName = provinceName;
        this.districtCode = districtCode;
        this.districtName = districtName;
        this.wardCode = wardCode;
        this.wardName = wardName;
        this.streetAddress = streetAddress;
        this.defaultAddress = false;
    }
}
