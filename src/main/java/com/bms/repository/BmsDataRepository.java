package com.bms.repository;

import com.bms.model.BmsData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BmsDataRepository extends JpaRepository<BmsData, Long> {
    
    // 최신 BMS 데이터 조회
    @Query("SELECT b FROM BmsData b ORDER BY b.timestamp DESC")
    List<BmsData> findLatestData();
    
    // 특정 시간 범위의 데이터 조회
    List<BmsData> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);
    
    // 최신 데이터 하나만 조회
    @Query("SELECT b FROM BmsData b ORDER BY b.timestamp DESC LIMIT 1")
    BmsData findTopByOrderByTimestampDesc();
}
