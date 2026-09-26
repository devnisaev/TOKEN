package com.tokenrealty.indexer.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigInteger;

@Entity
@Table(name = "indexer_cursors")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class IndexerCursor {

    public static final String DEFAULT_ID = "main";

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "last_block_number", nullable = false)
    private BigInteger lastBlockNumber;
}
