package com.dev.ecommerce.service;

import com.dev.ecommerce.dto.request.AddressRequest;
import com.dev.ecommerce.dto.response.AddressResponse;
import com.dev.ecommerce.entity.Address;
import com.dev.ecommerce.entity.User;
import com.dev.ecommerce.exception.BusinessException;
import com.dev.ecommerce.exception.ResourceNotFoundException;
import com.dev.ecommerce.repository.AddressRepository;
import com.dev.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean shouldBeDefault = Boolean.TRUE.equals(request.getSetAsDefault())
                || addressRepository.countByUserId(userId) == 0;

        if (shouldBeDefault) {
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = new Address(
                user,
                request.getRecipientName(),
                request.getPhone(),
                request.getProvinceCode(),
                request.getProvinceName(),
                request.getDistrictCode(),
                request.getDistrictName(),
                request.getWardCode(),
                request.getWardName(),
                request.getStreetAddress()
        );
        address.setDefaultAddress(shouldBeDefault);
        address.setLabel(request.getLabel());

        return toResponse(addressRepository.save(address));
    }

    @Transactional(readOnly = true)
    public List<AddressResponse> listByUser(Long userId) {
        return addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponse getById(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));
        return toResponse(address);
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        boolean shouldBeDefault = Boolean.TRUE.equals(request.getSetAsDefault());
        if (shouldBeDefault && !address.isDefaultAddress()) {
            addressRepository.clearDefaultForUser(userId);
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhone(request.getPhone());
        address.setProvinceCode(request.getProvinceCode());
        address.setProvinceName(request.getProvinceName());
        address.setDistrictCode(request.getDistrictCode());
        address.setDistrictName(request.getDistrictName());
        address.setWardCode(request.getWardCode());
        address.setWardName(request.getWardName());
        address.setStreetAddress(request.getStreetAddress());
        address.setLabel(request.getLabel());
        address.setDefaultAddress(shouldBeDefault || address.isDefaultAddress());

        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        boolean wasDefault = address.isDefaultAddress();
        addressRepository.delete(address);

        if (wasDefault) {
            addressRepository.findByUserIdOrderByDefaultAddressDescCreatedAtDesc(userId)
                    .stream()
                    .findFirst()
                    .ifPresent(first -> {
                        first.setDefaultAddress(true);
                        addressRepository.save(first);
                    });
        }
    }

    @Transactional
    public AddressResponse setDefault(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", addressId));

        addressRepository.clearDefaultForUser(userId);
        address.setDefaultAddress(true);
        return toResponse(addressRepository.save(address));
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .provinceCode(address.getProvinceCode())
                .provinceName(address.getProvinceName())
                .districtCode(address.getDistrictCode())
                .districtName(address.getDistrictName())
                .wardCode(address.getWardCode())
                .wardName(address.getWardName())
                .streetAddress(address.getStreetAddress())
                .fullAddress(buildFullAddress(address))
                .defaultAddress(address.isDefaultAddress())
                .label(address.getLabel())
                .build();
    }

    private String buildFullAddress(Address address) {
        return address.getStreetAddress() + ", " +
                address.getWardName() + ", " +
                address.getDistrictName() + ", " +
                address.getProvinceName();
    }
}
