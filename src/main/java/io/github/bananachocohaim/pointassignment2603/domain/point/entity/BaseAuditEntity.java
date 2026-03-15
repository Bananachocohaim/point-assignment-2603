package io.github.bananachocohaim.pointassignment2603.domain.point.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;        //최초생성일시

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;        //최종갱신일시

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;               //최초생성식별자

    @LastModifiedBy
    private String updatedBy;               //최종갱신식별자
}
