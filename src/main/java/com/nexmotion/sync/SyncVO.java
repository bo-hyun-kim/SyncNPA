package com.nexmotion.sync;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SyncVO {
    private LocalDateTime chgstartdate;
    private LocalDateTime chgenddate;
}